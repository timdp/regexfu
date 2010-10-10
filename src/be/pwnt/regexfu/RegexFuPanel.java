/*
 * Regex-Fu! v0.3
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
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
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
 * @version 0.3
 * @author tim@pwnt.be
 */
public class RegexFuPanel extends JSplitPane implements ActionListener,
		DocumentListener {
	private static final long serialVersionUID = 2732315785172243459L;

	private static final String PRODUCT_NAME = "Regex-Fu!";

	private static final String PRODUCT_VERSION = "0.3";

	private static final int PATTERN_LINES = 4;

	private static final int PATTERN_COLUMNS = 80;

	private static final int REGEX_SPACING = 5;

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

	private JButton prevButton;

	private JButton nextButton;

	private JLabel historyIndexLabel;

	private JTextArea subjectArea;

	private JTextArea resultArea;

	private JButton firstMatchButton;

	private JButton nextMatchButton;

	private JButton resetButton;

	private Color defaultColor;

	private HighlightPainter[] highlightPainters;

	private Matcher matcher;

	private int matchCount;

	private List<Pattern> history;

	private int historyIndex;

	private ResourceBundle bundle;

	public RegexFuPanel() {
		super(VERTICAL_SPLIT);
		bundle = ResourceBundle.getBundle(getClass().getCanonicalName());
		createActions();
		buildInterface();
		history = new Vector<Pattern>();
		reset();
	}

	private void reset() {
		regexArea.setText("");
		history.clear();
		historyIndex = -1;
		updateHistoryView();
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
			flags |= button.effectiveFlag();
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
			history.add(pattern);
			historyIndex = history.size() - 1;
			updateHistoryView();
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
			String title = MessageFormat.format(bundle.getString("matchTitle"),
					++matchCount);
			resultArea.append(MessageFormat.format(
					bundle.getString("matchHeader"), title, start, end));
			String groupMatchFormat = bundle.getString("groupMatch");
			for (int i = 1; i <= matcher.groupCount(); i++) {
				resultArea.append("\n"
						+ MessageFormat.format(groupMatchFormat, i,
								matcher.start(i), matcher.end(i),
								matcher.group(i)));
			}
			try {
				subjectArea.getHighlighter().addHighlight(start, end, hp);
				resultArea.getHighlighter().addHighlight(oldLength,
						oldLength + title.length(), hp);
			} catch (BadLocationException e) {
			}
		} else {
			resultArea.append(bundle.getString(matchCount == 0 ? "noMatches"
					: "noMoreMatches"));
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

	private void seek(int delta) {
		historyIndex += delta;
		Pattern pattern = history.get(historyIndex);
		regexArea.setText(pattern.pattern());
		for (ModifierButton button : modifierButtons) {
			button.setSelected((button.flag() & pattern.flags()) != 0);
		}
		updateHistoryView();
		problemChanged();
	}

	private void updateHistoryView() {
		prevButton.setEnabled(historyIndex > 0);
		nextButton.setEnabled(historyIndex < history.size() - 1);
		historyIndexLabel.setText(history.isEmpty() ? "" : ""
				+ (historyIndex + 1));
	}

	private void buildInterface() {
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
		prevButton = new JButton("" + (char) 0x25B2);
		prevButton.addActionListener(this);
		historyIndexLabel = new JLabel("", JLabel.CENTER);
		nextButton = new JButton("" + (char) 0x25BC);
		nextButton.addActionListener(this);
		JPanel historyPanel = new JPanel(new BorderLayout(0, 0));
		historyPanel.add(prevButton, BorderLayout.PAGE_START);
		historyPanel.add(historyIndexLabel, BorderLayout.CENTER);
		historyPanel.add(nextButton, BorderLayout.PAGE_END);
		JPanel optionsPanel = new JPanel(new BorderLayout(REGEX_SPACING, 0));
		optionsPanel.add(modifiersPanel, BorderLayout.CENTER);
		optionsPanel.add(historyPanel, BorderLayout.LINE_END);
		JPanel topPanel = new JPanel(new BorderLayout(REGEX_SPACING, 0));
		topPanel.setBorder(BorderFactory.createTitledBorder(bundle
				.getString("regularExpressionTitle")));
		regexArea = newTextArea(PATTERN_LINES, PATTERN_COLUMNS);
		regexArea.getDocument().addDocumentListener(this);
		topPanel.add(new JScrollPane(regexArea), BorderLayout.CENTER);
		topPanel.add(optionsPanel, BorderLayout.LINE_END);
		add(topPanel);
		JSplitPane bottomPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder());
		bottomPanel.setResizeWeight(0.5);
		subjectArea = newTextArea(SUBJECT_LINES, 1);
		subjectArea.getDocument().addDocumentListener(this);
		JPanel subjectPanel = new JPanel(new BorderLayout(0, 0));
		subjectPanel.setBorder(BorderFactory.createTitledBorder(bundle
				.getString("subjectTitle")));
		subjectPanel.add(new JScrollPane(subjectArea), BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 0, 0));
		firstMatchButton = new JButton(getActionMap().get(
				bundle.getString("firstMatchLabel")));
		firstMatchButton.setMnemonic(bundle.getString("firstMatchMnemonic")
				.charAt(0));
		buttonPanel.add(firstMatchButton);
		nextMatchButton = new JButton(getActionMap().get(
				bundle.getString("nextMatchLabel")));
		nextMatchButton.setMnemonic(bundle.getString("nextMatchMnemonic")
				.charAt(0));
		buttonPanel.add(nextMatchButton);
		buttonPanel.add(new JLabel());
		resetButton = new JButton(getActionMap().get(
				bundle.getString("resetLabel")));
		resetButton.setMnemonic(bundle.getString("resetMnemonic").charAt(0));
		buttonPanel.add(resetButton);
		subjectPanel.add(buttonPanel, BorderLayout.PAGE_END);
		bottomPanel.add(subjectPanel);
		resultArea = newTextArea(RESULT_LINES, 1);
		resultArea.setEditable(false);
		JPanel resultPanel = new JPanel(new BorderLayout(0, 0));
		resultPanel.setBorder(BorderFactory.createTitledBorder(bundle
				.getString("resultsTitle")));
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

	private void createActions() {
		addAction(new AbstractAction(bundle.getString("firstMatchLabel")) {
			private static final long serialVersionUID = -1033343899331562399L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (firstMatchButton.isEnabled()) {
					findFirstMatch();
				}
			}
		}, bundle.getString("firstMatchKeyCode"));
		addAction(new AbstractAction(bundle.getString("nextMatchLabel")) {
			private static final long serialVersionUID = -4469381847245861076L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (nextMatchButton.isEnabled()) {
					findNextMatch();
				} else if (firstMatchButton.isEnabled()) {
					findFirstMatch();
				}
			}
		}, bundle.getString("nextMatchKeyCode"));
		addAction(new AbstractAction(bundle.getString("resetLabel")) {
			private static final long serialVersionUID = -9196429518139767257L;

			@Override
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		}, bundle.getString("resetKeyCode"));
	}

	private void addAction(Action action, String key) {
		String name = (String) action.getValue(Action.NAME);
		getActionMap().put(name, action);
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),
				name);
	}

	private static JTextArea newTextArea(int rows, int cols) {
		JTextArea area = new JTextArea(rows, cols);
		// Enable tabbing
		area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
				null);
		area.setFocusTraversalKeys(
				KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
		return area;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (prevButton.equals(e.getSource())) {
			seek(-1);
		} else if (nextButton.equals(e.getSource())) {
			seek(1);
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
			setMnemonic(modifier);
			this.flag = flag;
		}

		public int flag() {
			return flag;
		}

		public int effectiveFlag() {
			return isSelected() ? flag() : 0;
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
