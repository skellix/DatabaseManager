import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;

public class Util {
	
	public static DefaultMutableTreeNode navigateTree(DefaultMutableTreeNode rootTreeNode, String ... strings) {
		
		Iterator<String> it = Arrays.asList(strings).iterator();
		Enumeration e = rootTreeNode.children();
		
		String itValue = it.next();
		
		while (e.hasMoreElements()) {
			
			Object node = e.nextElement();
			
			if (node instanceof DefaultMutableTreeNode) {
				
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node;
				
				Object userObject = childNode.getUserObject();
				
				if (userObject.equals(itValue)) {
					
					if (!it.hasNext()) {
						
						return childNode;
					}
					itValue = it.next();
					e = childNode.children();
					continue;
				}
			}
			break;
		}
		return null;
	}

}
