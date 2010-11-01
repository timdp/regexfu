/*
 * Regex-Fu! v0.4.1
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
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
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
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;

/**
 * It's Regex-Fu!
 * 
 * @version 0.4.1
 * @author tim@pwnt.be
 */
public class RegexFuPanel extends JSplitPane implements ActionListener,
		KeyListener, DocumentListener, CaretListener {
	private static final String PRODUCT_NAME = "Regex-Fu!";

	private static final String PRODUCT_VERSION = "0.4.1";

	private static final Dimension DEFAULT_SIZE = new Dimension(700, 500);

	private static final int SPACING = 5;

	private static final Color[] HIGHLIGHT_COLORS = new Color[] { Color.yellow,
			Color.cyan, Color.magenta };

	private static final Color ERROR_COLOR = Color.red;

	private static final Color BRACKET_MATCH_COLOR = Color.green;

	private static final char[] MODIFIERS = new char[] { 'i', 'm', 's', 'x' };

	private static final int[] FLAGS = new int[] { Pattern.CASE_INSENSITIVE,
			Pattern.MULTILINE, Pattern.DOTALL, Pattern.COMMENTS };

	private static final Set<Character> OPENING_BRACES = new HashSet<Character>();

	private static final Map<Character, Character> CLOSING_BRACES = new HashMap<Character, Character>();

	private static final HighlightPainter[] HIGHLIGHT_PAINTERS = new HighlightPainter[HIGHLIGHT_COLORS.length];

	private static final HighlightPainter BRACE_PAINTER = new DefaultHighlightPainter(
			BRACKET_MATCH_COLOR);

	private static final long serialVersionUID = 2732315785172243459L;

	static {
		CLOSING_BRACES.put(')', '(');
		CLOSING_BRACES.put(']', '[');
		CLOSING_BRACES.put('}', '{');
		OPENING_BRACES.addAll(CLOSING_BRACES.values());
		for (int i = 0; i < HIGHLIGHT_COLORS.length; i++) {
			HIGHLIGHT_PAINTERS[i] = new DefaultHighlightPainter(
					HIGHLIGHT_COLORS[i]);
		}
	}

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

	private Matcher matcher;

	private int matchCount;

	private List<Pattern> history;

	private int historyIndex;

	private Map<Integer, Integer> braceCache;

	private ResourceBundle bundle;

	public RegexFuPanel() {
		super(VERTICAL_SPLIT);
		bundle = ResourceBundle.getBundle(getClass().getCanonicalName());
		history = new Vector<Pattern>();
		braceCache = new HashMap<Integer, Integer>();
		createActions();
		buildInterface();
		setPreferredSize(DEFAULT_SIZE);
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
			HighlightPainter hp = HIGHLIGHT_PAINTERS[matchCount
					% HIGHLIGHT_PAINTERS.length];
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
		prevButton.setEnabled(previousExists());
		nextButton.setEnabled(nextExists());
		historyIndexLabel.setText(history.isEmpty() ? " " : ""
				+ (historyIndex + 1));
	}

	private boolean previousExists() {
		return historyIndex > 0;
	}

	private boolean nextExists() {
		return historyIndex < history.size() - 1;
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
		historyIndexLabel = new JLabel(" ", JLabel.CENTER);
		nextButton = new JButton("" + (char) 0x25BC);
		nextButton.addActionListener(this);
		JPanel historyPanel = new JPanel(new BorderLayout(0, SPACING));
		historyPanel.add(prevButton, BorderLayout.PAGE_START);
		historyPanel.add(historyIndexLabel, BorderLayout.CENTER);
		historyPanel.add(nextButton, BorderLayout.PAGE_END);
		JPanel optionsPanel = new JPanel(new BorderLayout(SPACING, 0));
		optionsPanel.add(modifiersPanel, BorderLayout.CENTER);
		optionsPanel.add(historyPanel, BorderLayout.LINE_END);
		JPanel topPanel = new JPanel(new BorderLayout(SPACING, 0));
		topPanel.setBorder(BorderFactory.createTitledBorder(bundle
				.getString("regularExpressionTitle")));
		regexArea = newTextArea();
		regexArea.getDocument().addDocumentListener(this);
		regexArea.addCaretListener(this);
		regexArea.addKeyListener(this);
		topPanel.add(new JScrollPane(regexArea), BorderLayout.CENTER);
		topPanel.add(optionsPanel, BorderLayout.LINE_END);
		add(topPanel);
		JSplitPane bottomPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		bottomPanel.setBorder(BorderFactory.createEmptyBorder());
		bottomPanel.setResizeWeight(0.5);
		subjectArea = newTextArea();
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
		resultArea = newTextArea();
		resultArea.setEditable(false);
		JPanel resultPanel = new JPanel(new BorderLayout(0, 0));
		resultPanel.setBorder(BorderFactory.createTitledBorder(bundle
				.getString("resultsTitle")));
		resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
		bottomPanel.add(resultPanel);
		add(bottomPanel);
		defaultColor = resultArea.getForeground();
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

	private void updateBraceCache() {
		braceCache.clear();
		String regex = regexArea.getText();
		if (regex.length() < 2)
			return;
		Map<Character, Stack<Integer>> stacks = new HashMap<Character, Stack<Integer>>();
		for (char c : OPENING_BRACES) {
			stacks.put(c, new Stack<Integer>());
		}
		char first = regex.charAt(0);
		if (OPENING_BRACES.contains(first)) {
			stacks.get(first).push(0);
		}
		for (int i = 1; i < regex.length(); i++) {
			if (regex.charAt(i - 1) != '\\'
					|| (i > 1 && regex.charAt(i - 2) == '\\')) {
				char c = regex.charAt(i);
				if (OPENING_BRACES.contains(c)) {
					stacks.get(c).push(i);
				} else if (CLOSING_BRACES.containsKey(c)
						&& !stacks.get(CLOSING_BRACES.get(c)).isEmpty()) {
					int other = stacks.get(CLOSING_BRACES.get(c)).pop();
					braceCache.put(other, i);
					braceCache.put(i, other);
				}
			}
		}
	}

	private void updateBraceHighlights() {
		removeBraceHighlights();
		int pos = regexArea.getCaretPosition();
		if (!highlightBrace(pos - 1)) {
			highlightBrace(pos);
		}
	}
	
	private void removeBraceHighlights() {
		Highlighter hl = regexArea.getHighlighter();
		for (Highlight h : hl.getHighlights()) {
			if (h.getPainter() == BRACE_PAINTER) {
				hl.removeHighlight(h);
			}
		}
	}

	private boolean highlightBrace(int pos) {
		if (!braceCache.containsKey(pos)) {
			return false;
		}
		int other = braceCache.get(pos);
		Highlighter hl = regexArea.getHighlighter();
		try {
			hl.addHighlight(pos, pos + 1, BRACE_PAINTER);
			hl.addHighlight(other, other + 1, BRACE_PAINTER);
		} catch (BadLocationException ex) {
		}
		return true;
	}

	private static JTextArea newTextArea() {
		JTextArea area = new JTextArea();
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
	public void keyPressed(KeyEvent e) {
		if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
			if (e.getKeyCode() == KeyEvent.VK_UP) {
				if (previousExists()) {
					seek(-1);
				}
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				if (nextExists()) {
					seek(1);
				}
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		problemChanged();
		updateBraceCache();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		updateBraceCache();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		updateBraceHighlights();
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
