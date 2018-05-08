import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.skellix.database.table.ColumnType;
import com.skellix.database.table.ExperimentalTable;
import com.skellix.database.table.RowFormat;
import com.skellix.database.table.RowFormatter;
import com.skellix.database.table.RowFormatterException;

public class DatabaseManagerWindow {
	
	private JFrame frame = new JFrame("");
	private JTabbedPane tabbs;
	private JTextPane sourceArea = new JTextPane();
	private JTextPane outputArea = new JTextPane();
	
	private DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
	private DefaultTreeModel treeModel = new DefaultTreeModel(rootTreeNode);
	private JTree tree = new JTree(treeModel);
	
	private List<Database> databases = new ArrayList<>();
	
	private ActionListener addAddTabActionListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			addQueryTab();
		}
	};
	
	private void addQueryTab() {
		
		int index = tabbs.getTabCount () - 1;
		String title = "untitled_" + String.valueOf(index) + ".query";
		JPanel newTab = queryEditorTab();
		tabbs.insertTab(title, null, newTab, null, index);
		tabbs.setSelectedComponent(newTab);
		
		tabbs.setTabComponentAt(index, tabLabelWithCloseButton(newTab, title));
	}

	public void show() {
		
		frame.add(Menu.create(rootTreeNode, treeModel, tree, databases, frame, this), BorderLayout.NORTH);
		frame.add(statusPanel(), BorderLayout.SOUTH);
		
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, explorerView(), editorView());
		split.setDividerLocation(200);
		frame.add(split, BorderLayout.CENTER);
		frame.setSize(600, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		Map<Object, Object> hints = new LinkedHashMap<>();
		hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		((Graphics2D) frame.getGraphics()).addRenderingHints(hints);
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
		
		tabbs = new JTabbedPane();
		
		addAddTab();
		
		panel.add(tabbs, BorderLayout.CENTER);
		return panel;
	}
	
	private Component tabLabelWithCloseButton(JPanel tab, String title) {
		JPanel panel = new JPanel();
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

	private void addAddTab() {
		tabbs.addTab("+", null);
		
		JPanel addPanel = new JPanel();
		addPanel.setOpaque(false);
		
		JButton addTab = new JButton("+");
	    addTab.setOpaque(false);
	    addTab.setBorder(null);
	    addTab.setContentAreaFilled(false);
	    addTab.setFocusPainted(false);

	    addTab.setFocusable(false);

	    addPanel.add(addTab);
	    
	    tabbs.setTabComponentAt(tabbs.getTabCount () - 1, addPanel);
	    
	    addTab.addActionListener(addAddTabActionListener);
	}

	private JPanel queryEditorTab() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, queryEditorInput(), editorOutput());
		split.setDividerLocation(500);
		panel.add(split);
		return panel;
	}

	private JPanel queryEditorInput() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JScrollPane jScrollPane = new JScrollPane(new JPanel(new BorderLayout()){{
			add(sourceArea);
		}});
		LineNumberPanel lineNumberPanel = new LineNumberPanel(sourceArea, jScrollPane);
		
		panel.add(lineNumberPanel, BorderLayout.WEST);
		panel.add(jScrollPane, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel editorOutput() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		outputArea.setEditable(false);
		outputArea.setBackground(Color.LIGHT_GRAY);
		outputArea.setForeground(Color.DARK_GRAY);
		JScrollPane jScrollPane = new JScrollPane(new JPanel(new BorderLayout()){{
			add(outputArea);
		}});
		panel.add(jScrollPane, BorderLayout.CENTER);
		return panel;
	}

	public void newTableEditorTab(Database database) {
		
		int index = tabbs.getTabCount () - 1;
		String title = "table_" + String.valueOf(index);
		JPanel newTab = tableEditorTab(database);
		tabbs.insertTab(title, null, newTab, null, index);
		tabbs.setSelectedComponent(newTab);
		
		tabbs.setTabComponentAt(index, tabLabelWithCloseButton(newTab, title));
	}
	
	private JPanel tableEditorTab(Database database) {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(TableEditor.tableEditorInput(database, panel, tabbs, tree, treeModel, rootTreeNode));
		return panel;
	}

	private JPanel addColumn() {
		JPanel columnConfig = new JPanel();
		columnConfig.setLayout(new BoxLayout(columnConfig, BoxLayout.X_AXIS));
		
		return columnConfig;
	}

}
