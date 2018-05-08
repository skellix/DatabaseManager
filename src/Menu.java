import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class Menu {
	
	private DefaultMutableTreeNode rootTreeNode;
	private DefaultTreeModel treeModel;
	private JTree tree;
	private JFrame frame;
	private DatabaseManagerWindow databaseManagerWindow;

	private Menu(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel, JTree tree, JFrame frame, DatabaseManagerWindow databaseManagerWindow) {
		
		this.rootTreeNode = rootTreeNode;
		this.treeModel = treeModel;
		this.tree = tree;
		this.frame = frame;
		this.databaseManagerWindow = databaseManagerWindow;
	}

	public static Component create(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel, JTree tree, JFrame frame, DatabaseManagerWindow databaseManagerWindow) {
		Menu menu = new Menu(rootTreeNode, treeModel, tree, frame, databaseManagerWindow);
		return menu.menuBar();
	}
	
	private JMenuBar menuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu());
		menuBar.add(editMenu());
		menuBar.add(tableMenu());
		menuBar.add(queryMenu());
		menuBar.add(helpMenu());
		return menuBar;
	}
	
	AbstractAction newDatabaseAction = new AbstractAction("New Database") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			JFileChooser fileChooser = new JFileChooser(".");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(null);
			
			if (result == JFileChooser.APPROVE_OPTION) {
				
				File file = fileChooser.getSelectedFile();
				
				if ((file.exists() && file.isDirectory()) || file.mkdir()) {
					
					Database database = Database.getOrCreate(file.toPath(), rootTreeNode, treeModel, tree, databaseManagerWindow.databases);
					
					if (database != null) {
						
						databaseManagerWindow.databases.add(database);
					}
					
				} else {
					
					System.err.println("ERROR: unable to create database directory: " +
							file.getAbsolutePath().toString());
				}
				
			} else if (result == JFileChooser.CANCEL_OPTION) {
				
				return;
				
			} else if (result == JFileChooser.ERROR_OPTION) {
				
				System.err.println("ERROR: unable to create database file");
			}
		}
	};
	
	AbstractAction openDatabaseAction = new AbstractAction("Open Database") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			JFileChooser fileChooser = new JFileChooser(".");
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = fileChooser.showOpenDialog(null);
			
			if (result == JFileChooser.APPROVE_OPTION) {
				
				File file = fileChooser.getSelectedFile();
				
				if (file.exists() && file.isDirectory()) {
					
					Database database = Database.getOrCreate(file.toPath(), rootTreeNode, treeModel, tree, databaseManagerWindow.databases);
					
					if (database != null) {
						
						databaseManagerWindow.databases.add(database);
					}
					
				} else {
					
					System.err.println("ERROR: unable to open database directory: " +
							file.getAbsolutePath().toString());
				}
				
			} else if (result == JFileChooser.CANCEL_OPTION) {
				
				return;
				
			} else if (result == JFileChooser.ERROR_OPTION) {
				
				System.err.println("ERROR: unable to create database file");
			}
		}
	};
	
	AbstractAction deleteDatabaseAction = new AbstractAction("Delete Database") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
//			JDialog dialog = new JDialog((JFrame) null, "Are you sure", true);
//			dialog.add(new JTextArea("Are you sure you want to delete the '" + "" + "' database?"));
		}
	};
	
	AbstractAction exitAction = new AbstractAction("Exit") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	};

	private JMenu fileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(new JMenuItem(newDatabaseAction));
		menu.add(new JMenuItem(openDatabaseAction));
		menu.add(new JMenuItem(deleteDatabaseAction));
		menu.add(new JMenuItem(exitAction));
		return menu;
	}
	
	AbstractAction settingsAction = new AbstractAction("Settings") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private JMenu editMenu() {
		JMenu menu = new JMenu("Edit");
		menu.add(new JMenuItem(settingsAction));
		return menu;
	}
	
	AbstractAction createTableAction = new AbstractAction("Create Table") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (databaseManagerWindow.databases.isEmpty()) {
				
				noDatabasesFound();
				return;
			}
			
			chooseDatabase(database -> {
				
				databaseManagerWindow.newTableEditorTab(database);
			});
		}
	};
	
	private void chooseDatabase(Consumer<Database> then) {
		
		JDialog dialog = new JDialog((JFrame) null, "Select a database", true);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		for (Database database : databaseManagerWindow.databases) {
			
			panel.add(new JButton(new AbstractAction(database.getDatabasePath().getFileName().toString()) {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					dialog.setVisible(false);
					then.accept(database);
				}
			}));
		}
		
		dialog.add(new JScrollPane(panel), BorderLayout.CENTER);
		dialog.add(new JButton(new AbstractAction("Cancel") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		}), BorderLayout.SOUTH);
		
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
	
	AbstractAction importTableAction = new AbstractAction("Import Table") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private JMenu tableMenu() {
		JMenu menu = new JMenu("Table");
		menu.add(new JMenuItem(createTableAction));
		menu.add(new JMenuItem(importTableAction));
		return menu;
	}
	
	protected void noDatabasesFound() {
		
		JDialog dialog = new JDialog((JFrame) null, "No databases Found", true);
		JTextArea message = new JTextArea("You must create or import a database first!");
		message.setForeground(Color.RED);
		dialog.add(message, BorderLayout.CENTER);
		dialog.add(new JButton(new AbstractAction("Close") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		}), BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	AbstractAction createQueryAction = new AbstractAction("Create Query") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	AbstractAction importQueryAction = new AbstractAction("Import Query") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private JMenu queryMenu() {
		JMenu menu = new JMenu("Query");
		menu.add(new JMenuItem(createQueryAction));
		menu.add(new JMenuItem(importQueryAction));
		return menu;
	}
	
	AbstractAction helpAction = new AbstractAction("Help") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	AbstractAction aboutAction = new AbstractAction("About") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JDialog dialog = AboutPanel.create();
			dialog.setVisible(true);
		}
	};
	
	private JMenu helpMenu() {
		JMenu menu = new JMenu("Help");
		menu.add(new JMenuItem(helpAction));
		menu.add(new JMenuItem(aboutAction));
		return menu;
	}

}
