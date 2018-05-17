import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.skellix.database.row.RowFormatter;
import com.skellix.database.row.RowFormatterException;
import com.skellix.database.table.Table;

public class Database {
	
	private Path databasePath;
	private Path tablesDir;
	private Path queriesDir;

	private Database() {
		
		//
	}

	private Database(Path databasePath, Path tablesDir, Path queriesDir) {
		
		this.tablesDir = tablesDir;
		this.queriesDir = queriesDir;
		this.databasePath = databasePath;
	}

	public static Database getOrCreate(Path databasePath, DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel, JTree tree, List<Database> databases) {
		
		Path tablesDir = databasePath.resolve("tables");
		
		if (!Files.exists(tablesDir)) {
			
			try {
				Files.createDirectories(tablesDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Path queriesDir = databasePath.resolve("queries");
		
		if (!Files.exists(queriesDir)) {
			
			try {
				Files.createDirectories(queriesDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Database database = new Database(databasePath, tablesDir, queriesDir);
		
		for (Enumeration e = rootTreeNode.children() ; e.hasMoreElements() ;) {
			
			Object node = e.nextElement();
			
			if (node instanceof DefaultMutableTreeNode) {
				
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node;
				Object userObject = childNode.getUserObject();
				if (userObject.equals("databases")) {
					
					String databaseName = databasePath.getFileName().toString();
					
					DefaultMutableTreeNode databaseNode = new DefaultMutableTreeNode(databaseName);
					childNode.add(databaseNode);
					
					DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode("tables");
					databaseNode.add(tablesNode);
					
					try {
						Files.list(tablesDir).forEach(path -> {
							
							String name = path.getFileName().toString();
							DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(name);
							tablesNode.add(tableNode);
						});
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					DefaultMutableTreeNode queriesNode = new DefaultMutableTreeNode("queries");
					databaseNode.add(queriesNode);
					
					try {
						Files.list(queriesDir).forEach(path -> {
							
							String name = path.getFileName().toString();
							DefaultMutableTreeNode queryNode = new DefaultMutableTreeNode(name);
							queriesNode.add(queryNode);
						});
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					treeModel.reload(childNode);
					tree.expandPath(new TreePath(databaseNode.getPath()));
					treeModel.reload(childNode);
				}
			}
		}
		
		return database;
	}
	
	public String getName() {
		
		return getDatabasePath().getFileName().toString();
	}
	
	public Path getDatabasePath() {
		
		return databasePath;
	}
	
	public Path getTablesDir() {
		
		return tablesDir;
	}
	
	public Path getQueriesDir() {
		
		return queriesDir;
	}

	public List<Table> tables() {
		
		try {
			return Files.list(tablesDir)
					.map(directory -> {
						
						try {
							return Table.getOrCreate(directory, RowFormatter.parse(Table.getFormatPath(directory)));
						} catch (IOException e) {
							e.printStackTrace();
						} catch (RowFormatterException e) {
							e.printStackTrace();
						}
						return null;
					})
					.filter(predicate -> predicate != null)
					.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void delete() {
		
		try {
			Files.list(getTablesDir()).forEach(tablePath -> {
				
				try {
					Table.deleteTable(tablePath);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Files.list(getQueriesDir()).forEach(queryPath -> {
				
				try {
					Files.delete(queryPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Path tablesDir = getTablesDir();
			System.out.println("deleting: " + tablesDir.toAbsolutePath().toString());
			tablesDir.toFile().delete();
			Files.delete(tablesDir);
			waitForDelete(tablesDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Path queriesDir = getQueriesDir();
			System.out.println("deleting: " + queriesDir.toAbsolutePath().toString());
			Files.delete(queriesDir);
			waitForDelete(queriesDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Files.delete(getDatabasePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void waitForDelete(Path path) throws IOException {
		
		long start = System.currentTimeMillis();
		
		while (System.currentTimeMillis() - start < 5000) {
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if (!Files.exists(path)) {
				
				return;
			}
		}
		
		throw new IOException("Unable to delete file: " + path.toAbsolutePath().toString());
	}

}
