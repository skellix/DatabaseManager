import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.skellix.database.row.RowFormat;
import com.skellix.database.row.TableRow;
import com.skellix.database.session.Session;
import com.skellix.database.table.Table;
import com.skellix.database.table.TableFormat;
import com.skellix.database.table.query.QueryNodeParser;
import com.skellix.database.table.query.exception.QueryParseException;
import com.skellix.database.table.query.node.NumberQueryNode;
import com.skellix.database.table.query.node.QueryNode;
import com.skellix.database.table.query.node.StringQueryNode;
import com.skellix.database.table.query.node.ValueQueryNode;

import treeparser.TreeNode;

public class QueryEditor {

	private Database database;
	private JPanel panel;
	private JTabbedPane tabbs;
	private DatabaseManagerWindow databaseManagerWindow;
	
	private JTextPane sourceArea = new JTextPane();
	private JTextPane outputArea = new JTextPane();
	JLabel status = new JLabel();
	JTextField queryName = new JTextField();
	private static Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	private ScheduledFuture<?> scheduledParse;
	
	{
		
		queryName.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				
				validateQueryName();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				
				validateQueryName();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
				validateQueryName();
			}
		});
		
		sourceArea.setFont(font);
		outputArea.setFont(font);
		
		Style errorStyle = sourceArea.addStyle("error", null);
		Style plainStyle = sourceArea.addStyle("plain", null);
		Style constantStyle = sourceArea.addStyle("constant", null);
		Style operatorStyle = sourceArea.addStyle("operator", null);
		Style keywordStyle = sourceArea.addStyle("keyword", null);
		
		StyleConstants.setForeground(errorStyle, Color.RED);
		StyleConstants.setBackground(errorStyle, Color.PINK);
		StyleConstants.setItalic(errorStyle, true);
		
		StyleConstants.setForeground(constantStyle, new Color(128, 0, 0));
		StyleConstants.setForeground(operatorStyle, new Color(0, 128, 0));
		StyleConstants.setForeground(keywordStyle, new Color(0, 128, 255));
		
		ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				tryParseQuery();
			}
		};
		
		sourceArea.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				
				if (scheduledParse != null) {
				
					scheduledParse.cancel(true);
				}
				scheduledParse = scheduledExecutorService.schedule(runnable, 500, TimeUnit.MILLISECONDS);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				
				if (scheduledParse != null) {
					
					scheduledParse.cancel(true);
				}
				scheduledParse = scheduledExecutorService.schedule(runnable, 500, TimeUnit.MILLISECONDS);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
				// Do nothing
			}
		});
	}
	
	AbstractAction executeAction = new AbstractAction("Execute") {
		
		boolean queryHeaderPrinted = false;
		RowFormat resultRowFormat = null;
		int rowCount = 0;
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			rowCount = 0;
			queryHeaderPrinted = false;
			
			String source = sourceArea.getText();
			try (Session session = Session.createNewSession(true)) {
				
				session.setStartDirectory(database.getTablesDir());
				QueryNode queryNode = QueryNodeParser.parse(source);
				
				long queryStart = System.currentTimeMillis();
				
				Object result = queryNode.query(session);
				
				if (result instanceof Table) {
					Stream<TableRow> stream = ((Table) result).stream();
					
					StringBuilder sb = new StringBuilder();
					
					stream.forEach(row -> {
						
						RowFormat rowFormat = row.rowFormat;
						
						String rowString = rowFormat.columnNames.stream().map(columnName -> {
							
							int byteSize = rowFormat.columnSizes.get(columnName);
							String formatString = rowFormat.columnTypes.get(columnName).formatString(byteSize);
							Object value = row.columns.get(rowFormat.columnIndexes.get(columnName)).get();
							
							if (!queryHeaderPrinted) {
								
								resultRowFormat = rowFormat;
								sb.append(rowFormat.getHeader(TableFormat.FORMAT_CELLS));
								queryHeaderPrinted = true;
							}
							
							return String.format(formatString, value);
						}).collect(Collectors.joining("|"));
						sb.append('|');
						sb.append(rowString);
						sb.append("|\n");
						
						rowCount ++;
					});
					
					if (resultRowFormat != null) {
					
						sb.append(resultRowFormat.getEnd(TableFormat.FORMAT_CELLS));
					}
					
					outputArea.setText(sb.toString());
				}
				
				long queryEnd = System.currentTimeMillis();
				long queryTime = queryEnd - queryStart;
				status.setText(String.format("retrieved %d rows in %d ms", rowCount, queryTime));
				
			} catch (QueryParseException e) {
				outputArea.setText(e.toString());
				e.printStackTrace();
			} catch (Exception e) {
				outputArea.setText(e.toString());
				e.printStackTrace();
			}
		}
	};
	
	AbstractAction saveAction = new AbstractAction("Save") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			String nameString = queryName.getText();
			
			if (!queryNameIsValid()) {
				
				return;
			}
			
			String source = sourceArea.getText();
			Path queryPath = database.getQueriesDir().resolve(nameString);
			
			try {
				Files.write(queryPath, source.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				
				DefaultMutableTreeNode node = Util.navigateTree(databaseManagerWindow.rootTreeNode, "databases", database.getName(), "queries");
				
				if (node != null) {
					
					for (Enumeration<DefaultMutableTreeNode> children = node.children() ; children.hasMoreElements() ;) {
						
						DefaultMutableTreeNode childNode = children.nextElement();
						
						String userString = childNode.toString();
						
						if (userString.equals(nameString)) {
							
							return;
						}
					}
					
					DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(nameString);
					node.add(newChild);
					
					databaseManagerWindow.treeModel.reload(node);
					databaseManagerWindow.tree.expandPath(new TreePath(node.getPath()));
					databaseManagerWindow.treeModel.reload(node);
				}
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public QueryEditor(String title, Database database, JPanel panel, JTabbedPane tabbs, DatabaseManagerWindow databaseManagerWindow) {
		
		this.database = database;
		this.panel = panel;
		this.tabbs = tabbs;
		this.databaseManagerWindow = databaseManagerWindow;
		
		queryName.setText(title);
		queryName.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				
				int index = tabbs.indexOfComponent(panel);
				tabbs.setTabComponentAt(index, DatabaseManagerWindow.tabLabelWithCloseButton(tabbs, panel, queryName.getText()));
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				
				int index = tabbs.indexOfComponent(panel);
				tabbs.setTabComponentAt(index, DatabaseManagerWindow.tabLabelWithCloseButton(tabbs, panel, queryName.getText()));
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				
				int index = tabbs.indexOfComponent(panel);
				tabbs.setTabComponentAt(index, DatabaseManagerWindow.tabLabelWithCloseButton(tabbs, panel, queryName.getText()));
			}
		});
	}
	
	protected void validateQueryName() {
		
		queryName.setBackground(Color.WHITE);
		
		if (!queryNameIsValid()) {
			
			queryName.setBackground(Color.PINK);
		}
	}

	private boolean queryNameIsValid() {
		
		String text = queryName.getText();
		
		if (text.length() == 0) {
			
			return false;
		}
		
		for (char c : text.toCharArray()) {
			
			boolean isValid = Character.isLetterOrDigit(c) || c == '-' || c == '_';
			
			if (!isValid) {
				
				return false;
			}
		}
		
		return true;
	}

	public void setSource(String source) {
		
		sourceArea.setText(source);
	}

	protected void tryParseQuery() {
		
		String source = sourceArea.getText();
		
		StyledDocument doc = sourceArea.getStyledDocument();
		
		Caret carat = sourceArea.getCaret();
		int caratDot = carat.getDot();
//		int caratMark = carat.getMark();
		
		try {
			TreeNode queryNode = QueryNodeParser.parseTree(source);
			
			List<TreeNode> nodes = QueryNodeParser.forAllDescendantsOrSelf(queryNode).collect(Collectors.toList());
			
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					
					doc.setCharacterAttributes(0, doc.getLength(), doc.getStyle("plain"), true);
					
					for (TreeNode node : nodes) {
						
						if (node instanceof QueryNode) {
							
							if (node instanceof StringQueryNode || node instanceof NumberQueryNode || node instanceof ValueQueryNode) {
								
								doc.setCharacterAttributes(node.start - 1, node.end - node.start + 3, doc.getStyle("constant"), true);
								doc.setCharacterAttributes(node.end + 2, 1, doc.getStyle("plain"), true);
							
							} else {
								
								doc.setCharacterAttributes(node.start, node.end - node.start + 1, doc.getStyle("keyword"), true);
								doc.setCharacterAttributes(node.end + 1, 1, doc.getStyle("plain"), true);
							}
						} else if (node.hasParent()) {
							
							doc.setCharacterAttributes(node.start, node.end - node.start + 1, doc.getStyle("error"), true);
							doc.setCharacterAttributes(node.end + 1, 1, doc.getStyle("plain"), true);
						}
					}
				}
			});
		} catch (QueryParseException e1) {
			System.err.println(e1.getMessage());
		}
		
//		sourceArea.select(caratMark, caratDot);
		carat.setDot(caratDot);
	}

	public static QueryEditor queryEditorInput(String title, Database database, JPanel panel, JTabbedPane tabbs, DatabaseManagerWindow databaseManagerWindow) {
		
		return new QueryEditor(title, database, panel, tabbs, databaseManagerWindow);
	}
	
	public Component getComponent() {
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, queryEditorInput(), editorOutput());
		split.setOpaque(false);
		split.setBorder(null);
		split.setDividerLocation(200);
		return split;
	}
	
	private JPanel topPanel() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.add(new JLabel("Database: " + database.getName()));
		panel.add(queryName);
//		panel.add(Box.createHorizontalGlue());
		JButton saveButton = new JButton(saveAction);
		saveButton.setContentAreaFilled(false);
		panel.add(saveButton);
		return panel;
	}

	private JPanel queryEditorInput() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		JPanel sourcePanel = new JPanel(new BorderLayout());
		sourcePanel.add(sourceArea);
		
		JScrollPane jScrollPane = new JScrollPane(sourcePanel);
		LineNumberPanel lineNumberPanel = new LineNumberPanel(sourceArea, jScrollPane);
		
		panel.add(topPanel(), BorderLayout.NORTH);
		panel.add(lineNumberPanel, BorderLayout.WEST);
		panel.add(jScrollPane, BorderLayout.CENTER);
		JButton executeButton = new JButton(executeAction);
		executeButton.setContentAreaFilled(false);
		panel.add(executeButton, BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel editorOutput() {
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		outputArea.setEditable(false);
		outputArea.setBackground(Color.WHITE);
		outputArea.setForeground(Color.DARK_GRAY);
		
		JPanel outputPanel = new JPanel(new BorderLayout());
		outputPanel.add(outputArea);
		
		JScrollPane jScrollPane = new JScrollPane(outputPanel);
		
		panel.add(jScrollPane, BorderLayout.CENTER);
		panel.add(status, BorderLayout.SOUTH);
		return panel;
	}

}
