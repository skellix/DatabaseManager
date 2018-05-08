import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

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
					tree.expandPath(new TreePath(tablesNode.getPath()));
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

}
