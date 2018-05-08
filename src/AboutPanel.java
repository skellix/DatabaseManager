import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class AboutPanel {
	
	public static JDialog create() {
		
		JDialog dialog = new JDialog((JFrame) null, "About", true);
		URL aboutUrl = Menu.class.getClassLoader().getResource("about.txt");
		
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		
		try (InputStream in = aboutUrl.openStream()) {
			
			int length = 0;
			byte[] buffer = new byte[1024];
			StringBuilder sb = new StringBuilder();
			
			while ((length = in.read(buffer)) != -1) {
				
				sb.append(new String(buffer, 0, length));
			}
			textArea.setText(sb.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		dialog.add(textArea, BorderLayout.CENTER);
		dialog.add(new JButton(new AbstractAction("Close") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		}), BorderLayout.SOUTH);
		dialog.setLocationRelativeTo(null);
		dialog.setSize(300, 200);
		
		return dialog;
	}

}
