import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class TreeUi implements MouseListener {

	private JTree tree;
	private Menu menu;

	public TreeUi(JTree tree, Menu menu) {
		
		this.tree = tree;
		this.menu = menu;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
		int row = tree.getClosestRowForLocation(e.getX(), e.getY());
		tree.setSelectionRow(row);
		TreePath selectionPath = tree.getSelectionPath();
		Object[] path = selectionPath.getPath();
		
		if (SwingUtilities.isRightMouseButton(e)) {
			
			if (path.length > 0) {
				
				if (path[0].toString().equals("")) {
					
					if (path.length > 1) {
					
						if (path[1].toString().equals("databases")) {
							
							if (path.length > 2) {
							
								String database = path[2].toString();
								
								if (path.length > 3) {
									
									if (path[3].toString().equals("tables")) {
										
										if (path.length > 4) {
											
											String table = path[4].toString();
											JPopupMenu popupMenu = new JPopupMenu("table: " + table);
											popupMenu.add(new JMenuItem(menu.editTable(database, table)));
											popupMenu.add(new JMenuItem(menu.queryTable(database, table)));
											popupMenu.add(new JMenuItem(menu.addRowToTable(database, table)));
											popupMenu.add(new JMenuItem(menu.deleteTable(database, table)));
											popupMenu.show(e.getComponent(), e.getX(), e.getY());
											
										} else {
											
											// tables selected
											JPopupMenu popupMenu = new JPopupMenu("tables");
											popupMenu.add(new JMenuItem(menu.createTable(database)));
											popupMenu.show(e.getComponent(), e.getX(), e.getY());
										}
										
									} else if (path[3].toString().equals("queries")) {
										
										if (path.length > 4) {
											
											String query = path[4].toString();
											JPopupMenu popupMenu = new JPopupMenu("query: " + query);
											popupMenu.add(new JMenuItem(menu.deleteQuery(database, query)));
											popupMenu.show(e.getComponent(), e.getX(), e.getY());
											
										} else {
											
											// queries selected
											JPopupMenu popupMenu = new JPopupMenu("queries");
											popupMenu.add(new JMenuItem(menu.createQuery(database)));
											popupMenu.show(e.getComponent(), e.getX(), e.getY());
										}
									}
									
								} else {
									
									// database selected
									JPopupMenu popupMenu = new JPopupMenu("database");
									popupMenu.add(new JMenuItem(menu.createTable(database)));
									popupMenu.add(new JMenuItem(menu.createQuery(database)));
									popupMenu.add(new JMenuItem(menu.deleteDatabase(database)));
									popupMenu.show(e.getComponent(), e.getX(), e.getY());
								}
								
							} else {
								
								// databases selected
								JPopupMenu popupMenu = new JPopupMenu("databases");
								popupMenu.add(new JMenuItem(menu.createDatabaseAction));
								popupMenu.add(new JMenuItem(menu.importDatabaseAction));
								popupMenu.show(e.getComponent(), e.getX(), e.getY());
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
