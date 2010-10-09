/*
 * Regex-Fu! v0.2
 * Created by Tim De Pauw <http://pwnt.be/>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.pwnt.regexfu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.HighlightPainter;

/**
 * It's Regex-Fu!
 * 
 * @version 0.2
 * @author tim@pwnt.be
 */
public class RegexFuPanel extends JSplitPane implements ActionListener,
		DocumentListener {
	private static final long serialVersionUID = 2732315785172243459L;

	private static final String PRODUCT_NAME = "Regex-Fu!";

	private static final String PRODUCT_VERSION = "0.2";

	private static final int REGEX_LINES = 3;

	private static final int REGEX_COLUMNS = 80;

	private static final int SUBJECT_LINES = 12;

	private static final int RESULT_LINES = 8;

	private static final Color[] HIGHLIGHT_COLORS = new Color[] { Color.yellow,
			Color.cyan, Color.magenta };

	private static final Color ERROR_COLOR = Color.red;

	private static final char[] MODIFIERS = new char[] { 'i', 'm', 's', 'x' };

	private static final int[] FLAGS = new int[] { Pattern.CASE_INSENSITIVE,
			Pattern.MULTILINE, Pattern.DOTALL, Pattern.COMMENTS };

	private JTextArea regexArea;

	private List<ModifierButton> modifierButtons;

	private JTextArea subjectArea;

	private JTextArea resultArea;

	private JButton firstMatchButton;

	private JButton nextMatchButton;

	private JButton resetButton;

	private Color defaultColor;

	private HighlightPainter[] highlightPainters;

	private Matcher matcher;

	private int matchCount;

	public RegexFuPanel() {
		super(VERTICAL_SPLIT);
		build();
		reset();
	}

	private void reset() {
		regexArea.setText("");
		subjectArea.setText("");
		for (ModifierButton button : modifierButtons) {
			button.setSelected(false);
		}
		problemChanged();
	}

	private void findFirstMatch() {
		problemChanged();
		String regex = regexArea.getText();
		String subject = subjectArea.getText();
		int flags = 0;
		for (ModifierButton button : modifierButtons) {
			flags |= button.getFlag();
		}
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(regex, flags);
		} catch (Exception ex) {
			resultArea.setForeground(ERROR_COLOR);
			resultArea.setText(ex.getMessage());
		}
		if (pattern != null) {
			matcher = pattern.matcher(subject);
			nextMatchButton.setEnabled(true);
			matchCount = 0;
			findNextMatch();
		}
	}

	private void findNextMatch() {
		if (matchCount > 0) {
			resultArea.append("\n\n");
		}
		if (matcher.find()) {
			HighlightPainter hp = highlightPainters[matchCount
					% highlightPainters.length];
			int start = matcher.start(), end = matcher.end();
			int oldLength = resultArea.getText().length();
			String title = "Match #" + ++matchCount;
			resultArea.append(title + ": " + start + "-" + end);
			for (int i = 1; i <= matcher.groupCount(); i++) {
				resultArea.append(String.format("\n[%d] %d-%d = |%s|", i,
						matcher.start(i), matcher.end(i), matcher.group(i)));
			}
			try {
				subjectArea.getHighlighter().addHighlight(start, end, hp);
				resultArea.getHighlighter().addHighlight(oldLength,
						oldLength + title.length(), hp);
			} catch (BadLocationException e) {
			}
		} else {
			resultArea.append(matchCount == 0 ? "No matches"
					: "No more matches");
			nextMatchButton.setEnabled(false);
		}
	}

	private void problemChanged() {
		matcher = null;
		firstMatchButton.setEnabled(!regexArea.getText().isEmpty()
				&& !subjectArea.getText().isEmpty());
		nextMatchButton.setEnabled(false);
		subjectArea.getHighlighter().removeAllHighlights();
		resultArea.setText("");
		resultArea.setForeground(defaultColor);
	}

	private void build() {
		setBorder(BorderFactory.createEmptyBorder());
		JPanel modifiersPanel = new JPanel(new GridLayout(2,
				(MODIFIERS.length + 1) / 2, 0, 0));
		modifierButtons = new Vector<ModifierButton>(4);
		for (int i = 0; i < MODIFIERS.length; i++) {
			ModifierButton button = new ModifierButton(MODIFIERS[i], FLAGS[i]);
			button.addActionListener(this);
			modifierButtons.add(button);
			modifiersPanel.add(button);
		}
		JPanel topPanel = new JPanel(new BorderLayout(0, 0));
		topPanel.setBorder(BorderFactory
				.createTitledBorder("Regular Expression"));
		regexArea = new JTextArea(REGEX_LINES, REGEX_COLUMNS);
		regexArea.setLineWrap(true);
		regexArea.setWrapStyleWord(false);
		regexArea.getDocument().addDocumentListener(this);
		topPanel.add(new JScrollPane(regexArea), BorderLayout.CENTER);
		topPanel.add(modifiersPanel, BorderLayout.LINE_END);
		add(topPanel);
		JSplitPane bottomPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder());
		bottomPanel.setResizeWeight(0.5);
		subjectArea = new JTextArea(SUBJECT_LINES, 1);
		subjectArea.setLineWrap(false);
		subjectArea.getDocument().addDocumentListener(this);
		JPanel subjectPanel = new JPanel(new BorderLayout(0, 0));
		subjectPanel.setBorder(BorderFactory.createTitledBorder("Subject"));
		subjectPanel.add(new JScrollPane(subjectArea), BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 0, 0));
		firstMatchButton = new JButton("First Match");
		firstMatchButton.setMnemonic('f');
		firstMatchButton.addActionListener(this);
		buttonPanel.add(firstMatchButton);
		nextMatchButton = new JButton("Next Match");
		nextMatchButton.setMnemonic('n');
		nextMatchButton.addActionListener(this);
		buttonPanel.add(nextMatchButton);
		buttonPanel.add(new JLabel());
		resetButton = new JButton("Reset");
		resetButton.setMnemonic('r');
		resetButton.addActionListener(this);
		buttonPanel.add(resetButton);
		subjectPanel.add(buttonPanel, BorderLayout.PAGE_END);
		bottomPanel.add(subjectPanel);
		resultArea = new JTextArea(RESULT_LINES, 1);
		resultArea.setEditable(false);
		JPanel resultPanel = new JPanel(new BorderLayout(0, 0));
		resultPanel.setBorder(BorderFactory.createTitledBorder("Result"));
		resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
		bottomPanel.add(resultPanel);
		add(bottomPanel);
		defaultColor = resultArea.getForeground();
		highlightPainters = new HighlightPainter[HIGHLIGHT_COLORS.length];
		for (int i = 0; i < HIGHLIGHT_COLORS.length; i++) {
			highlightPainters[i] = new DefaultHighlightPainter(
					HIGHLIGHT_COLORS[i]);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (firstMatchButton.equals(e.getSource())) {
			findFirstMatch();
		} else if (nextMatchButton.equals(e.getSource())) {
			findNextMatch();
		} else if (resetButton.equals(e.getSource())) {
			reset();
		} else if (e.getSource() instanceof JToggleButton) {
			problemChanged();
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		problemChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		problemChanged();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
	}

	private static class ModifierButton extends JToggleButton {
		private static final long serialVersionUID = -8270951366369990628L;

		private int flag;

		public ModifierButton(char modifier, int flag) {
			super("" + modifier);
			this.flag = flag;
		}

		public int getFlag() {
			return isSelected() ? flag : 0;
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getSystemLookAndFeelClassName());
				} catch (Exception e) {
				}
				JFrame frame = new JFrame(PRODUCT_NAME + " " + PRODUCT_VERSION);
				frame.setContentPane(new RegexFuPanel());
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}
}
