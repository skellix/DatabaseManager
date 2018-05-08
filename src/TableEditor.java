import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.skellix.database.table.ColumnType;
import com.skellix.database.table.ExperimentalTable;
import com.skellix.database.table.RowFormat;
import com.skellix.database.table.RowFormatter;
import com.skellix.database.table.RowFormatterException;

import treeparser.TreeNode;

public class TableEditor {
	
	public static JPanel tableEditorInput(Database database, JPanel tab, JTabbedPane tabbs, JTree tree, DefaultTreeModel treeModel, DefaultMutableTreeNode rootTreeNode) {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
		namePanel.add(new JLabel("Table Name:"));
		JTextField nameField = new JTextField();
		namePanel.add(nameField);
		panel.add(namePanel, BorderLayout.NORTH);
		
		JPanel columnsPanel = new JPanel();
		columnsPanel.setLayout(new BorderLayout());
		
		TableConfigModel model = new TableConfigModel();
		
		JTable table = new JTable(model);
		TableColumnModel columnModel = table.getColumnModel();
		
		TableColumn typeColumn = columnModel.getColumn(0);
		typeColumn.setHeaderValue("Type");
		
		
		JComboBox<ColumnType> types = new JComboBox<>(ColumnType.values());
		ListCellRenderer<ColumnType> renderer = new ListCellRenderer<ColumnType>() {
			
			DefaultListCellRenderer listRenderer = new DefaultListCellRenderer();

			@Override
			public Component getListCellRendererComponent(JList<? extends ColumnType> list, ColumnType value, int index, boolean isSelected, boolean cellHasFocus) {
				
				listRenderer = (DefaultListCellRenderer) listRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				listRenderer.setIcon(null);
				listRenderer.setText(((ColumnType) value).name());
				return listRenderer;
			}
		};
		types.setRenderer(renderer);
		
		TableCellEditor editor = new DefaultCellEditor(types);
		
		typeColumn.setCellRenderer(new TableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				
				if (value instanceof ColumnType) {
					
					ColumnType columnType = (ColumnType) value;
					return new JLabel(columnType.name());
				}
				
				return new JLabel("NULL");
			}
		});
		typeColumn.setCellEditor(editor);
		
		TableColumn nameColumn = columnModel.getColumn(1);
		nameColumn.setHeaderValue("Name");
		
		TableColumn sizeColumn = columnModel.getColumn(2);
		sizeColumn.setHeaderValue("Size");
		
		TableColumn removeColumn = columnModel.getColumn(3);
		removeColumn.setHeaderValue("Remove Column");
		removeColumn.setCellEditor(new ButtonCellEditor(new JCheckBox()));
		removeColumn.setCellRenderer(new ButtonCellRenderer());
		
		columnsPanel.add(new JScrollPane(table));
		
		columnsPanel.add(new JButton(new AbstractAction("Add Row") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				model.addRow(model.createBasicRow());
				table.revalidate();
				table.repaint();
			}
		}), BorderLayout.SOUTH);
		
		panel.add(columnsPanel, BorderLayout.CENTER);
		
		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
		
		JTextPane compiledTableOutput = new JTextPane();
		
		Style error = compiledTableOutput.addStyle("error", null);
		Style plain = compiledTableOutput.addStyle("plain", null);
		Style tag = compiledTableOutput.addStyle("tag", null);
		Style constant = compiledTableOutput.addStyle("constant", null);
		Style variable = compiledTableOutput.addStyle("variable", null);
		Style function = compiledTableOutput.addStyle("function", null);
		Style comment = compiledTableOutput.addStyle("comment", null);
		
		StyleConstants.setForeground(error, Color.RED);
		StyleConstants.setBackground(error, Color.PINK);
		StyleConstants.setItalic(error, true);
		
		StyleConstants.setForeground(tag, new Color(235, 176, 53));
		StyleConstants.setBold(tag, true);
		
		StyleConstants.setForeground(constant, new Color(221, 30, 47));
		StyleConstants.setBold(constant, true);
		
		StyleConstants.setForeground(variable, new Color(6, 162, 203));
		
		StyleConstants.setForeground(function, new Color(33, 133, 89));
		
		StyleConstants.setForeground(comment, Color.GRAY);
		
		JPanel compilePanel = new JPanel();
		compilePanel.setLayout(new BorderLayout());
		compilePanel.add(compiledTableOutput, BorderLayout.CENTER);
		JButton createButton = new JButton(new AbstractAction("Create") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = nameField.getText();
				
				if (name.length() == 0) {
					
					System.err.println("ERROR: empty table name");
					return;
				}
				
				for (char c : name.toCharArray()) {
					
					if (Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '-') {
						
						continue;
					}
					
					System.err.println("ERROR: invalid character in table name '" + c + "' ");
					return;
				}
				Path tableDir = database.getTablesDir().resolve(name);
				RowFormat rowFormat;
				try {
					rowFormat = RowFormatter.parse(compiledTableOutput.getText());
				} catch (RowFormatterException e1) {
					System.err.println(e1.getMessage());
					return;
				}
				ExperimentalTable table = ExperimentalTable.getOrCreate(tableDir, rowFormat);
				
				DefaultMutableTreeNode treeNode = Util.navigateTree(
						rootTreeNode, "databases", database.getName(), "tables");
				
				DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(tableDir.getFileName().toString());
				treeNode.add(childNode);
				treeModel.reload(childNode);
				tree.expandPath(new TreePath(childNode.getPath()));
				treeModel.reload(childNode);
				
				int index = tabbs.indexOfComponent(tab);
				tabbs.removeTabAt(index);
			}
		});
		compilePanel.add(createButton, BorderLayout.SOUTH);
		DocumentListener editListener = new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				
				validateTableSettings(database, nameField, compiledTableOutput, createButton);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				
				validateTableSettings(database, nameField, compiledTableOutput, createButton);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
//				validateTableSettings(database, nameField, compiledTableOutput, createButton);
			}
		};
		compiledTableOutput.getDocument().addDocumentListener(editListener);
		nameField.getDocument().addDocumentListener(editListener);
		model.addChangeListener((tableConfigModel -> {
			
			compileTable(table, compiledTableOutput);
			validateTableSettings(database, nameField, compiledTableOutput, createButton);
		}));
		compileTable(table, compiledTableOutput);
		validateTableSettings(database, nameField, compiledTableOutput, createButton);
		
		panel.add(compilePanel, BorderLayout.SOUTH);
		return panel;
	}
	
	private static void compileTable(JTable table, JTextPane compiledTableOutput) {
		TableModel model = table.getModel();
		int rowCount = model.getRowCount();
		
		List<String> columnNames = new ArrayList<>();
		Map<String, ColumnType> columnTypes = new LinkedHashMap<>();
		Map<String, Integer> columnSizes = new LinkedHashMap<>();
		
		for (int i = 0 ; i < rowCount ; i ++) {
			
			ColumnType columnType = (ColumnType) model.getValueAt(i, 0);
			String columnName = (String) model.getValueAt(i, 1);
			Integer columnSize = Integer.parseInt((String) model.getValueAt(i, 2));
			
			columnNames.add(columnName);
			columnTypes.put(columnName, columnType);
			columnSizes.put(columnName, columnSize);
		}
		
		RowFormat format = new RowFormat(columnNames, columnTypes, columnSizes);
		
		compiledTableOutput.setText(format.toString());
		compiledTableOutput.revalidate();
		compiledTableOutput.repaint();
	}
	
	public static void validateTableSettings(Database database, JTextField nameField, JTextPane compiledTableOutput, JButton createButton) {
		String name = nameField.getText();
		String formatText = compiledTableOutput.getText();
		
		boolean error = false;
		
		createButton.setEnabled(false);
		nameField.setBackground(Color.WHITE);
		compiledTableOutput.setBackground(Color.WHITE);
		
		if (name.length() == 0) {
			
			nameField.setBackground(Color.PINK);
//			System.err.println("ERROR: empty table name");
			error = true;
			
		} else {
			
			for (char c : name.toCharArray()) {
				
				if (Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '-') {
					
					continue;
				}
				
				nameField.setBackground(Color.PINK);
//				System.err.println("ERROR: invalid character in table name '" + c + "' ");
				error = true;
			}
		}
		
		if (formatText.length() == 0) {
			
			compiledTableOutput.setBackground(Color.PINK);
//			System.err.println("ERROR: you need to compile the table first");
			error = true;
			
		} else {
			
			try {
				
				List<List<TreeNode>> codeParts = RowFormatter.getAst(formatText);
				
				List<TreeNode> types = codeParts.get(0);
				List<TreeNode> names = codeParts.get(1);
				List<TreeNode> sizes = codeParts.get(2);
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						
						StyledDocument doc = compiledTableOutput.getStyledDocument();
						
						for (TreeNode type : types) {
							
							doc.setCharacterAttributes(type.start, type.end - type.start + 1, doc.getStyle("function"), true);
							doc.setCharacterAttributes(type.end + 1, 1, doc.getStyle("plain"), true);
						}
						
						for (TreeNode size : sizes) {
							
							doc.setCharacterAttributes(size.start, size.end - size.start + 1, doc.getStyle("constant"), true);
							doc.setCharacterAttributes(size.end + 1, 1, doc.getStyle("plain"), true);
						}
					}
				});
				
			} catch (RowFormatterException e) {
				e.printStackTrace();
				error = true;
			}
		}
		
		if (error) {
			
			return;
		}
		
		try {
			
			RowFormat rowFormat = RowFormatter.parse(formatText);
			createButton.setEnabled(true);
			compiledTableOutput.setBackground(Color.WHITE);
			
		} catch (RowFormatterException e1) {
			System.err.println(e1.getMessage());
			compiledTableOutput.setBackground(Color.PINK);
			createButton.setEnabled(false);
			return;
		}
	}

}
