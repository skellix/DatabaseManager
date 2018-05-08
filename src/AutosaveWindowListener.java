import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class AutosaveWindowListener implements WindowListener {

	private static Path configPath = Paths.get(System.getProperty("user.home"), ".databaseManagerConfig");
	private static Charset charSet = Charset.forName("UTF-8");
	private DefaultMutableTreeNode rootTreeNode;
	private DefaultTreeModel treeModel;
	private JTree tree;
	private DatabaseManagerWindow databaseManagerWindow;

	public AutosaveWindowListener(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel, JTree tree, DatabaseManagerWindow databaseManagerWindow) {
		
		this.rootTreeNode = rootTreeNode;
		this.treeModel = treeModel;
		this.tree = tree;
		this.databaseManagerWindow = databaseManagerWindow;
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		
		List<CharSequence> lines = databaseManagerWindow.databases.stream().map(database -> database.getDatabasePath().toString()).collect(Collectors.toList());
		
		try {
			
			Files.write(configPath, lines, charSet, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		
		if (Files.exists(configPath)) {
			try {
				Files.readAllLines(configPath).stream().forEach(line -> {
					
					Path path = Paths.get(line);
					
					if (!Files.exists(path)) {
						
						System.err.println("ERROR: Unable to find database at path: " + line);
						return;
					}
					if (!Files.isDirectory(path)) {
						
						System.err.println("ERROR: Found file instead of directory at database path: " + line);
						return;
					}
					Database database = Database.getOrCreate(path, rootTreeNode, treeModel, tree, databaseManagerWindow.databases);
					
					if (database == null) {
						
						System.err.println("ERROR: Unable to create database from data at path: " + line);
						return;
					}
					databaseManagerWindow.databases.add(database);
				});
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}
