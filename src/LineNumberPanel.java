import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;


public class LineNumberPanel extends JPanel {

	private JTextPane textPane;
	private JScrollPane jScrollPane;

	public LineNumberPanel(JTextPane textPane, JScrollPane jScrollPane) {
		this.textPane = textPane;
		this.jScrollPane = jScrollPane;
		setFont(new Font(textPane.getFont().getName(), Font.BOLD, textPane.getFont().getSize()));
	}

	public void paint(Graphics g) {
		int fontHeight = getFontMetrics(textPane.getFont()).getHeight();
		int fontWidth = getFontMetrics(textPane.getFont()).getWidths()[' '];
		int lineStart = jScrollPane.getVerticalScrollBar().getValue() / fontHeight;
		int lineEnd = lineStart + (int) (jScrollPane.getViewport().getSize().getHeight() / fontHeight);
		int lineOffset = jScrollPane.getVerticalScrollBar().getValue() % fontHeight;
		int linesSize = textPane.getText().split("(?=\n)").length;
		
		setBorder(new EmptyBorder(0, ((""+linesSize).length()*fontWidth)-2, 0, 0));
		
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, ((""+lineEnd).length()+1)*fontWidth, (int) jScrollPane.getViewport().getSize().getHeight());
		g.setColor(Color.DARK_GRAY);
		int j = 1;
		for (int i = lineStart ; i < lineEnd+1 && i < linesSize ; i ++) {
			g.drawString(""+(i+1), 4, ((j++)*fontHeight)-lineOffset);
		}
	}
}
