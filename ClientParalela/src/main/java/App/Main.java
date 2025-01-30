

package App;

import javax.swing.JOptionPane;

import ChatUI.ChatUI;

public class Main {

	public static void main(String[] args) {
		String user = JOptionPane.showInputDialog(null, "Ingresa el usuario", "Emisor", JOptionPane.INFORMATION_MESSAGE);
		String receiver = JOptionPane.showInputDialog(null, "Ingresa el destinatario", "Receptor",
				JOptionPane.INFORMATION_MESSAGE);
		if (user != null && receiver != null) {
			ChatUI chatClient = new ChatUI(user, receiver);
			chatClient.setVisible(true);
		}
	}
}
