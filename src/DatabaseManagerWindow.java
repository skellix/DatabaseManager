import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;

public class DatabaseManagerWindow {
	
	private JFrame frame = new JFrame("");
	private JTabbedPane tabbs = new JTabbedPane();
	private JTextPane sourceArea = new JTextPane();
	private JTextPane outputArea = new JTextPane();
	
	public DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
	public DefaultTreeModel treeModel = new DefaultTreeModel(rootTreeNode);
	public JTree tree = new JTree(treeModel);
	private Menu menu;
	
	public List<Database> databases = new ArrayList<>();
	
	public DatabaseManagerWindow() {
		tree.setCellRenderer(new TreeCellRenderer() {
			
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
				
				JPanel panel = new JPanel();
				panel.setOpaque(false);
				panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
				
				Object userObject = node.getUserObject();
				
				if (userObject == null) {
					
					JLabel label = new JLabel("");
					label.setOpaque(false);
					panel.add(label);
					
				} else {
					
					TreeNode parent = node.getParent();
					
					if (parent instanceof DefaultMutableTreeNode) {
						DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
						Object parentNodeObject = parentNode.getUserObject();
						
						if (parentNodeObject != null && parentNodeObject.toString().equals("databases")) {
							
							panel.add(new JLabel(UIManager.getIcon("FileView.hardDriveIcon")));
						}
					}
					
					JLabel label = new JLabel(userObject.toString());
					label.setOpaque(false);
					panel.add(label);
				}
				
				return panel;
			}
		});
	}
	
	private ActionListener addTabActionListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			Object source = e.getSource();
			
			if (source instanceof JTabbedPane) {
				
				JTabbedPane tabbedPane = (JTabbedPane) source;
				JPopupMenu popupMenu = new JPopupMenu("Create");
				popupMenu.add(new JMenuItem(menu.createTableAction));
				popupMenu.add(new JMenuItem(menu.createQueryAction));
				Point pos = tabbedPane.getMousePosition();
				popupMenu.show(tabbedPane, pos.x, pos.y);
			}
		}
	};

	public void show() {
		
		menu = Menu.create(rootTreeNode, treeModel, tree, frame, this);
		frame.add(menu.menuBar(), BorderLayout.NORTH);
		frame.add(statusPanel(), BorderLayout.SOUTH);
		
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, explorerView(), editorView());
		split.setDividerLocation(200);
		frame.add(split, BorderLayout.CENTER);
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		Map<Object, Object> hints = new LinkedHashMap<>();
		hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		((Graphics2D) frame.getGraphics()).addRenderingHints(hints);
		
		frame.addWindowListener(new AutosaveWindowListener(rootTreeNode, treeModel, tree, this));
		
		tree.addMouseListener(new TreeUi(tree, menu));
	}

	private JPanel statusPanel() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		return panel;
	}

	private JPanel explorerView() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		DefaultMutableTreeNode databaseNode = new DefaultMutableTreeNode("databases");
		rootTreeNode.add(databaseNode);
		treeModel.reload(rootTreeNode);
		panel.add(tree, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel editorView() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(editorTabs());
		return panel;
	}

	private JPanel editorTabs() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		addAddTab();
		panel.add(tabbs, BorderLayout.CENTER);
		return panel;
	}

	private void addAddTab() {
		
		tabbs.addTab("+", null);
	    tabbs.addMouseListener(new OnPressMouseListener(e -> {
	    	
	    	int index = tabbs.indexOfTab("+");
	    	Rectangle bounds = tabbs.getUI().getTabBounds(tabbs, index);
	    	if (bounds.contains(e.getPoint())) {
	    		
	    		addTabActionListener.actionPerformed(new ActionEvent(tabbs, ActionEvent.ACTION_PERFORMED, "+"));
	    	}
	    }));
	}

	public TableEditor newTableEditorTab(Database database) {
		
		int index = tabbs.getTabCount () - 1;
		String title = "table_" + String.valueOf(index);
		
		JPanel tableEditorPanel = new JPanel();
		tableEditorPanel.setLayout(new BorderLayout());
		TableEditor tableEditor = TableEditor.tableEditorInput(database, tableEditorPanel, tabbs, tree, treeModel, rootTreeNode);
		tableEditorPanel.add(tableEditor.getComponent());
		
		tabbs.insertTab(title, null, tableEditorPanel, null, index);
		tabbs.setSelectedComponent(tableEditorPanel);
		
		tabbs.setTabComponentAt(index, tabLabelWithCloseButton(tabbs, tableEditorPanel, title));
		
		return tableEditor;
	}
	
	public QueryEditor newQueryEditorTab(Database database) {
		
		int index = tabbs.getTabCount () - 1;
		String title = "query_" + String.valueOf(index);
		
		JPanel queryEditorTab = new JPanel();
		queryEditorTab.setLayout(new BorderLayout());
		QueryEditor queryEditor = QueryEditor.queryEditorInput(title, database, queryEditorTab, tabbs, this);
		queryEditorTab.add(queryEditor.getComponent(), BorderLayout.CENTER);
		
		tabbs.insertTab(title, null, queryEditorTab, null, index);
		tabbs.setSelectedComponent(queryEditorTab);
		
		tabbs.setTabComponentAt(index, tabLabelWithCloseButton(tabbs, queryEditorTab, title));
		
		return queryEditor;
	}
	
	public static Component tabLabelWithCloseButton(JTabbedPane tabbs, JPanel tab, String title) {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel(title));
		JButton closeButton = new JButton(new AbstractAction("X") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				int index = tabbs.indexOfComponent(tab);
				tabbs.removeTabAt(index);
			}
		});
		closeButton.setOpaque(false);
		closeButton.setBorder(null);
	    closeButton.setContentAreaFilled(false);
	    closeButton.setFocusPainted(false);
		closeButton.setForeground(Color.RED);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(closeButton);
		return panel;
	}

}
