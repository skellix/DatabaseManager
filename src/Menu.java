import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.skellix.database.row.RowFormat;
import com.skellix.database.row.RowFormatter;
import com.skellix.database.row.RowFormatterException;
import com.skellix.database.table.Table;

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

	public static Menu create(DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel, JTree tree, JFrame frame, DatabaseManagerWindow databaseManagerWindow) {
		Menu menu = new Menu(rootTreeNode, treeModel, tree, frame, databaseManagerWindow);
		return menu;
	}
	
	public JMenuBar menuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu());
		menuBar.add(editMenu());
		menuBar.add(tableMenu());
		menuBar.add(queryMenu());
		menuBar.add(helpMenu());
		return menuBar;
	}
	
	AbstractAction createDatabaseAction = new AbstractAction("Create Database") {
		
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
	
	AbstractAction importDatabaseAction = new AbstractAction("Import Database") {
		
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
		menu.add(new JMenuItem(createDatabaseAction));
		menu.add(new JMenuItem(importDatabaseAction));
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
	
	AbstractAction deleteTableAction = new AbstractAction("Delete Table") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (databaseManagerWindow.databases.isEmpty()) {
				
				noDatabasesFound();
				return;
			}
			
			chooseDatabase(database -> {
				
				chooseTable(database, table -> {
					
					try {
						String name = table.getName();
						table.deleteTable();
						
						removeTableFromTree(database.getName(), name);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				});
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
	
	protected void removeTableFromTree(String databaseName, String tableName) {
		
		for (Enumeration e = rootTreeNode.children() ; e.hasMoreElements() ;) {
			
			Object node = e.nextElement();
			
			if (node instanceof DefaultMutableTreeNode) {
				
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node;
				Object userObject = childNode.getUserObject();
				
				if (userObject.equals("databases")) {
					
					for (Enumeration e1 = rootTreeNode.children() ; e1.hasMoreElements() ;) {
						
						Object node1 = e1.nextElement();
						
						if (node1 instanceof DefaultMutableTreeNode) {
							
							DefaultMutableTreeNode databaseNode = (DefaultMutableTreeNode) node1;
							Object databaseNodeUserObject = databaseNode.getUserObject();
							
							if (databaseNodeUserObject.equals(databaseName)) {
								
								for (Enumeration e2 = rootTreeNode.children() ; e2.hasMoreElements() ;) {
									
									Object node2 = e2.nextElement();
									
									if (node2 instanceof DefaultMutableTreeNode) {
										
										DefaultMutableTreeNode tableNode = (DefaultMutableTreeNode) node2;
										Object tableNodeUserObject = tableNode.getUserObject();
										
										if (tableNodeUserObject.equals(tableName)) {
											
											databaseNode.remove(tableNode);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void chooseTable(Database database, Consumer<Table> then) {
		
		JDialog dialog = new JDialog((JFrame) null, "Select a table", true);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		for (Table table : database.tables()) {
			
			panel.add(new JButton(new AbstractAction(table.getName()) {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					
					dialog.setVisible(false);
					then.accept(table);
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
		menu.add(new JMenuItem(deleteTableAction));
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
			
			chooseDatabase(database -> {
				
				databaseManagerWindow.newQueryEditorTab(database);
			});
		}
	};
	
	AbstractAction deleteQueryAction = new AbstractAction("Import Query") {
		
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
		menu.add(new JMenuItem(deleteQueryAction));
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

	public AbstractAction queryTable(String databaseName, String tableName) {
		
		return new AbstractAction("Query Table") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						QueryEditor queryEditor = databaseManagerWindow.newQueryEditorTab(database);
						
						Path tablePath = database.getTablesDir().resolve(tableName);

						StringBuilder sb = new StringBuilder();
						
						try {
							RowFormat rowFormat = RowFormatter.parse(Table.getFormatPath(tablePath));
							
							String last = null;
							
							for (String columnName : rowFormat.columnNames) {
								
								if (last != null) {
									
									sb.append(", ");
								}
								sb.append(columnName);
								last = columnName;
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (RowFormatterException e) {
							e.printStackTrace();
						}
						
						queryEditor.setSource("table '" + tableName + "' select " + sb.toString() + " limit 1000");
						return;
					}
				}
			}
		};
	}

	public AbstractAction deleteTable(String databaseName, String tableName) {
		
		return new AbstractAction("Delete Table") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						areYouSure("Are you sure you want to delete the '" + tableName + "' table?", () -> {
							
							// yes
							Path tablePath = database.getTablesDir().resolve(tableName);
							try {
								Table.deleteTable(tablePath);
								
								DefaultMutableTreeNode node = Util.navigateTree(databaseManagerWindow.rootTreeNode, "databases", database.getName(), "tables");
								
								if (node != null) {
									
									for (Enumeration<DefaultMutableTreeNode> children = node.children() ; children.hasMoreElements() ;) {
										
										DefaultMutableTreeNode childNode = children.nextElement();
										
										String userString = childNode.toString();
										
										if (userString.equals(tableName)) {
											
											node.remove(childNode);
											databaseManagerWindow.treeModel.reload(node);
											databaseManagerWindow.tree.expandPath(new TreePath(node.getPath()));
											databaseManagerWindow.treeModel.reload(node);
											return;
										}
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}, () -> {
							
							// no
						});
						return;
					}
				}
			}
		};
	}

	public AbstractAction createQuery(String databaseName) {
		
		return new AbstractAction("Create Query") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						databaseManagerWindow.newQueryEditorTab(database);
						return;
					}
				}
			}
		};
	}
	
	public AbstractAction deleteQuery(String databaseName, String query) {
		
		return new AbstractAction("Delete Query") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						Path queryPath = database.getQueriesDir().resolve(query);
						
						areYouSure("Are you sure you want to delete the '" + query + "' query?", () -> {
							
							// yes
							try {
								Files.delete(queryPath);
								
								DefaultMutableTreeNode node = Util.navigateTree(databaseManagerWindow.rootTreeNode, "databases", database.getName(), "queries");
								
								if (node != null) {
									
									for (Enumeration<DefaultMutableTreeNode> children = node.children() ; children.hasMoreElements() ;) {
										
										DefaultMutableTreeNode childNode = children.nextElement();
										
										String userString = childNode.toString();
										
										if (userString.equals(query)) {
											
											node.remove(childNode);
											databaseManagerWindow.treeModel.reload(node);
											databaseManagerWindow.tree.expandPath(new TreePath(node.getPath()));
											databaseManagerWindow.treeModel.reload(node);
											return;
										}
									}
								}
								
							} catch (IOException e) {
								e.printStackTrace();
							}
							
						}, () -> {
							
							// no
						});
						return;
					}
				}
			}
		};
	}

	public AbstractAction createTable(String databaseName) {
		
		return new AbstractAction("Create Table") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						databaseManagerWindow.newTableEditorTab(database);
						return;
					}
				}
			}
		};
	}

	public AbstractAction deleteDatabase(String databaseName) {
		
		return new AbstractAction("Delete Database") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						areYouSure("Are you sure you want to delete the '" + databaseName + "' database?", () -> {
							
							// yes
							database.delete();
							databaseManagerWindow.databases.remove(database);
							DefaultMutableTreeNode node = Util.navigateTree(rootTreeNode, "databases");
							
							for (Enumeration<DefaultMutableTreeNode> children = node.children() ; children.hasMoreElements() ;) {
								
								DefaultMutableTreeNode child = children.nextElement();
								String userString = child.toString();
								
								if (userString.equals(databaseName)) {
									
									node.remove(child);
									databaseManagerWindow.treeModel.reload(node);
									databaseManagerWindow.tree.expandPath(new TreePath(node.getPath()));
									databaseManagerWindow.treeModel.reload(node);
									return;
								}
							}
							
						}, () -> {
							
							// no
						});
						return;
					}
				}
			}
		};
	}
	
	
	public void areYouSure(String message, Runnable yes, Runnable no) {
		
		JDialog dialog = new JDialog((JFrame) null, "Are you sure?", true);
		JLabel messageLabel = new JLabel(message, UIManager.getIcon("OptionPane.warningIcon"), SwingConstants.LEFT);
		dialog.add(messageLabel, BorderLayout.NORTH);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		panel.add(new JButton(new AbstractAction("yes") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				dialog.setVisible(false);
				yes.run();
			}
		}));
		
		panel.add(new JButton(new AbstractAction("no") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				dialog.setVisible(false);
				no.run();
			}
		}));
		
		dialog.add(panel, BorderLayout.CENTER);
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

	public AbstractAction addRowToTable(String databaseName, String tableName) {
		
		return new AbstractAction("Add Row") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						QueryEditor queryEditor = databaseManagerWindow.newQueryEditorTab(database);
						
						try {
							Path tablePath = Files.list(database.getTablesDir())
								.filter(path -> path.getFileName().toString().equals(tableName))
								.findFirst().orElse(null);
							
							if (tablePath == null) {
								
								return;
							}
							
							Path rowFormatPath = Table.getFormatPath(tablePath);
							
							try {
								RowFormat rowFormat = RowFormatter.parse(rowFormatPath);
								queryEditor.setSource("table '" + tableName + "' addRow " + rowFormat.getInsertString());
								
							} catch (RowFormatterException e) {
								e.printStackTrace();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
		};
	}

	public AbstractAction editTable(String databaseName, String tableName) {
		
		return new AbstractAction("Edit Table") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				for (Database database : databaseManagerWindow.databases) {
					
					if (database.getName().equals(databaseName)) {
						
						QueryEditor queryEditor = databaseManagerWindow.newQueryEditorTab(database);
						
						try {
							Path tablePath = Files.list(database.getTablesDir())
								.filter(path -> path.getFileName().toString().equals(tableName))
								.findFirst().orElse(null);
							
							Path rowFormatPath = Table.getFormatPath(tablePath);
							
							try {
								RowFormat rowFormat = RowFormatter.parse(rowFormatPath);
								
								TableEditor tableEditor = databaseManagerWindow.newTableEditorTab(database);
								tableEditor.nameField.setText(tableName);
								tableEditor.compiledTableOutput.setText(rowFormat.toString());
//								queryEditor.setSource("table '" + tableName + "' addRow " + rowFormat.getInsertString());
								
							} catch (RowFormatterException e) {
								e.printStackTrace();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
		};
	}

}
