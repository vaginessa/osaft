package de.uni_hannover.osaft.plugins.connnectorappdata.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.Init;
import de.uni_hannover.osaft.adb.ADBThread;
import de.uni_hannover.osaft.plugininterfaces.ViewPlugin;
import de.uni_hannover.osaft.plugins.connnectorappdata.controller.ConnectorAppDataController;
import de.uni_hannover.osaft.plugins.connnectorappdata.tables.CustomDateCellRenderer;
import de.uni_hannover.osaft.plugins.connnectorappdata.tables.LiveSearchTableModel;
import de.uni_hannover.osaft.plugins.connnectorappdata.tables.TableColumnAdjuster;

//TODO: columns automatisch adjusten?
//TODO: um exceptions kümmern
//TODO: fokusproblem, wenn man einmal in die searchbar geklickt hat

@PluginImplementation
public class ConnectorAppDataView extends MouseAdapter implements ViewPlugin, ActionListener,
		ListSelectionListener, KeyListener, FocusListener {

	private JTabbedPane tabs;
	private JPanel calendarPanel, smsPanel, browserHPanel, browserSPanel, callPanel, contactPanel,
			mmsPanel, preferencesPanel;
	private MMSInfoPanel mmsInfo;
	private ContactInfoPanel contactInfo;
	private ConnectorAppDataController controller;
	private JButton openCSVButton, pushAppButton, pullCSVButton;
	private JFileChooser fc;
	private JScrollPane calendarScrollPane, callScrollPane, browserHScrollPane, browserSScrollPane,
			contactScrollPane, mmsScrollPane, smsScrollPane, contactInfoScroll;
	private JTable calendarTable, callTable, browserHTable, browserSTable, contactTable, mmsTable,
			smsTable;
	private Vector<JPanel> tabVector;
	private JTextArea smsTextArea;
	private JPopupMenu contextMenu;
	private JMenuItem copyCell, copyRow;
	private JTextField contactSearch, calendarSearch, browserHSearch, browserSSearch, smsSearch,
			mmsSearch, callsSearch;
	private ADBThread adb;

	// used for contextmenu
	private int currentX, currentY;
	private JTable currentTable;

	/**
	 * @wbp.parser.entryPoint
	 */
	@Init
	// called, when plugin is initialized
	public void init() {

		tabVector = new Vector<JPanel>();
		initGUI();
		fc = new JFileChooser();

		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		controller = new ConnectorAppDataController(this);
	}

	public void initGUI() {
		tabs = new JTabbedPane();

		calendarPanel = new JPanel(new BorderLayout(0, 0));
		calendarPanel.setName("Calendar");

		callPanel = new JPanel(new BorderLayout(0, 0));
		callPanel.setName("CallLogs");

		browserHPanel = new JPanel(new BorderLayout(0, 0));
		browserHPanel.setName("Browser History");

		browserSPanel = new JPanel(new BorderLayout(0, 0));
		browserSPanel.setName("Browser Search History");

		contactPanel = new JPanel(new BorderLayout(0, 0));
		contactPanel.setName("Contacts");

		mmsPanel = new JPanel(new BorderLayout(0, 0));
		mmsPanel.setName("MMS");

		smsPanel = new JPanel(new BorderLayout(0, 0));
		smsPanel.setName("SMS");

		ArrayList<JTable> tables = new ArrayList<JTable>();

		calendarTable = new JTable();
		callTable = new JTable();
		browserHTable = new JTable();
		browserSTable = new JTable();
		contactTable = new JTable();
		// selectionlistener listens for selection changes in the table
		contactTable.getSelectionModel().addListSelectionListener(this);
		mmsTable = new JTable();
		mmsTable.getSelectionModel().addListSelectionListener(this);
		smsTable = new JTable();
		smsTable.getSelectionModel().addListSelectionListener(this);
		tables.add(calendarTable);
		tables.add(callTable);
		tables.add(browserHTable);
		tables.add(browserSTable);
		tables.add(contactTable);
		tables.add(smsTable);
		tables.add(mmsTable);

		// Date objects will be rendered differently
		CustomDateCellRenderer cdcr = new CustomDateCellRenderer();

		for (int i = 0; i < tables.size(); i++) {
			tables.get(i).setDefaultRenderer(Date.class, cdcr);
			// columns are sortable:
			tables.get(i).setAutoCreateRowSorter(true);
			// reorder of columns is disabled
			tables.get(i).getTableHeader().setReorderingAllowed(false);
			tables.get(i).addMouseListener(this);
		}

		// put scrollpane-wrapped tables into panels, add listeners and
		// searchbars
		calendarScrollPane = new JScrollPane(calendarTable);
		calendarSearch = new JTextField("Search");
		calendarSearch.addKeyListener(this);
		calendarSearch.addFocusListener(this);
		calendarPanel.add(calendarSearch, BorderLayout.NORTH);
		calendarPanel.add(calendarScrollPane, BorderLayout.CENTER);

		callScrollPane = new JScrollPane(callTable);
		callsSearch = new JTextField("Search");
		callsSearch.addKeyListener(this);
		callsSearch.addFocusListener(this);
		callPanel.add(callsSearch, BorderLayout.NORTH);
		callPanel.add(callScrollPane, BorderLayout.CENTER);

		browserHScrollPane = new JScrollPane(browserHTable);
		browserHSearch = new JTextField("Search");
		browserHSearch.addFocusListener(this);
		browserHSearch.addKeyListener(this);
		browserHPanel.add(browserHSearch, BorderLayout.NORTH);
		browserHPanel.add(browserHScrollPane, BorderLayout.CENTER);

		browserSScrollPane = new JScrollPane(browserSTable);
		browserSSearch = new JTextField("Search");
		browserSSearch.addFocusListener(this);
		browserSSearch.addKeyListener(this);
		browserSPanel.add(browserSSearch, BorderLayout.NORTH);
		browserSPanel.add(browserSScrollPane, BorderLayout.CENTER);

		// contacts, mms and sms also provide an "info"-panel
		contactScrollPane = new JScrollPane(contactTable);
		JPanel contactWrapper = new JPanel(new BorderLayout());
		contactWrapper.add(contactScrollPane, BorderLayout.CENTER);
		contactSearch = new JTextField("Search");
		contactSearch.addKeyListener(this);
		contactSearch.addFocusListener(this);
		contactWrapper.add(contactSearch, BorderLayout.NORTH);
		contactPanel.add(contactWrapper, BorderLayout.CENTER);
		contactInfo = new ContactInfoPanel();
		contactInfo.setPreferredSize(new Dimension(280, 740));
		contactInfoScroll = new JScrollPane(contactInfo, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		contactInfoScroll.setPreferredSize(new Dimension(300, 0));
		contactPanel.add(contactInfoScroll, BorderLayout.EAST);

		mmsScrollPane = new JScrollPane(mmsTable);
		JPanel mmsWrapper = new JPanel(new BorderLayout());
		mmsWrapper.add(mmsScrollPane, BorderLayout.CENTER);
		mmsSearch = new JTextField("Search");
		mmsSearch.addKeyListener(this);
		mmsSearch.addFocusListener(this);
		mmsWrapper.add(mmsSearch, BorderLayout.NORTH);
		mmsPanel.add(mmsWrapper, BorderLayout.CENTER);
		mmsInfo = new MMSInfoPanel();
		mmsInfo.setPreferredSize(new Dimension(300, 0));
		mmsPanel.add(mmsInfo, BorderLayout.EAST);

		smsScrollPane = new JScrollPane(smsTable);
		JPanel smsWrapper = new JPanel(new BorderLayout());
		smsWrapper.add(smsScrollPane, BorderLayout.CENTER);
		smsSearch = new JTextField("Search");
		smsSearch.addKeyListener(this);
		smsSearch.addFocusListener(this);
		smsWrapper.add(smsSearch, BorderLayout.NORTH);
		smsPanel.add(smsWrapper, BorderLayout.CENTER);
		JPanel smsTextPanel = new JPanel(new BorderLayout());
		smsTextArea = new JTextArea();
		smsTextArea.setEditable(false);
		smsTextArea.setLineWrap(true);
		smsTextArea.setWrapStyleWord(true);
		smsTextPanel.add(new JScrollPane(smsTextArea));
		smsTextPanel.add(new JLabel("Text:"), BorderLayout.NORTH);
		smsTextPanel.setPreferredSize(new Dimension(300, 100));
		smsPanel.add(smsTextPanel, BorderLayout.EAST);

		openCSVButton = new JButton("Open CSV Files");
		openCSVButton.addActionListener(this);

		pushAppButton = new JButton("Push App to Phone");
		pushAppButton.addActionListener(this);

		pullCSVButton = new JButton("Pull CSV files from Phone");
		pullCSVButton.addActionListener(this);

		preferencesPanel = new JPanel();
		preferencesPanel.setName("Preferences");
		preferencesPanel.add(openCSVButton);
		preferencesPanel.add(pushAppButton);
		tabs.add(preferencesPanel);

		contextMenu = new JPopupMenu();
		copyCell = new JMenuItem("Copy cell to clipboard");
		copyCell.addActionListener(this);
		copyRow = new JMenuItem("Copy row to clipboard");
		copyRow.addActionListener(this);

		contextMenu.add(copyCell);
		contextMenu.add(copyRow);
	}

	@Override
	public String getName() {
		return "Connector App Data";
	}

	@Override
	public JComponent getView() {
		return tabs;
	}

	@Override
	public void triggered() {
		// TODO: soll was passieren?
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == openCSVButton) {
			// open filechooser
			int returnVal = fc.showOpenDialog(tabs);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File folder = fc.getSelectedFile();

				if (controller.iterateChosenFolder(folder)) {
					fc.setCurrentDirectory(folder);
				} else {
					JOptionPane.showMessageDialog(tabs, "No csv files found in this folder",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
				calendarScrollPane.validate();
			}
		}

		if (e.getSource() == pushAppButton) {
			JOptionPane.showMessageDialog(tabs,
					"Connector App will be pushed to phone. Please confirm install on the phone",
					"Information", JOptionPane.INFORMATION_MESSAGE);
			controller.pushApp();
		}

		if (e.getSource().equals(copyCell)) {
			controller.copySelectionToClipboard(currentX, currentY, currentTable, true);
		}
		if (e.getSource().equals(copyRow)) {
			controller.copySelectionToClipboard(currentX, currentY, currentTable, false);
		}
	}

	public void addTab(String sourceFile, LiveSearchTableModel model) {
		if (sourceFile.equals(ConnectorAppDataController.BROWSER_HISTORY_FILENAME)) {
			browserHTable.setModel(model);
			tabVector.add(browserHPanel);
		} else if (sourceFile.equals(ConnectorAppDataController.BROWSER_SEARCH_FILENAME)) {
			browserSTable.setModel(model);
			tabVector.add(browserSPanel);
		} else if (sourceFile.equals(ConnectorAppDataController.CALENDAR_FILENAME)) {
			calendarTable.setModel(model);
			tabVector.add(calendarPanel);
		} else if (sourceFile.equals(ConnectorAppDataController.CALLS_FILENAME)) {
			callTable.setModel(model);
			tabVector.add(callPanel);
		} else if (sourceFile.equals(ConnectorAppDataController.CONTACTS_FILENAME)) {
			contactTable.setModel(model);
			tabVector.add(contactPanel);
			contactTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			TableColumnAdjuster tca = new TableColumnAdjuster(contactTable);
			tca.adjustColumns();
		} else if (sourceFile.equals(ConnectorAppDataController.MMS_FILENAME)) {
			mmsTable.setModel(model);
			tabVector.add(mmsPanel);
			mmsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			TableColumnAdjuster tca = new TableColumnAdjuster(mmsTable);
			tca.adjustColumns();
			// mmsTable.getColumnModel().getColumn(0).setPreferredWidth(1);
			// mmsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
			// mmsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
			// mmsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
			// mmsTable.getColumnModel().getColumn(4).setPreferredWidth(1);
			// mmsTable.getColumnModel().getColumn(5).setPreferredWidth(1);
		} else if (sourceFile.equals(ConnectorAppDataController.SMS_FILENAME)) {
			smsTable.setModel(model);
			tabVector.add(smsPanel);
		}
	}

	// shows tabs in alphabetic order
	public void showTabs() {
		tabs.removeAll();
		tabs.add(preferencesPanel);
		Collections.sort(tabVector, new Comparator<JPanel>() {
			public int compare(JPanel p1, JPanel p2) {
				return p1.getName().compareTo(p2.getName());
			}
		});

		for (int i = tabVector.size() - 1; i >= 0; i--) {
			tabs.add(tabVector.get(i), 0);
		}

		tabVector.clear();
	}

	// update info-panels if selected row changed
	@Override
	public void valueChanged(ListSelectionEvent e) {
		int selectedRow;
		if (e.getSource().equals(smsTable.getSelectionModel())) {
			selectedRow = smsTable.getSelectedRow();
			if (selectedRow != -1) {
				smsTextArea.setText("" + smsTable.getValueAt(selectedRow, 3));
			}
		} else if (e.getSource().equals(mmsTable.getSelectionModel())) {
			selectedRow = mmsTable.getSelectedRow();
			if (selectedRow != -1) {
				String id = mmsTable.getValueAt(selectedRow, 0).toString();
				String text = mmsTable.getValueAt(selectedRow, 3).toString();
				boolean hasAttachment = (Boolean) mmsTable.getValueAt(selectedRow, 5);
				mmsInfo.setInfo(id, text, hasAttachment, fc.getCurrentDirectory());
			}
		} else if (e.getSource().equals(contactTable.getSelectionModel())) {
			selectedRow = contactTable.getSelectedRow();
			if (selectedRow != -1) {
				String id = contactTable.getValueAt(selectedRow, 0).toString();
				String name = contactTable.getValueAt(selectedRow, 1).toString();
				String numbers = contactTable.getValueAt(selectedRow, 2).toString();
				String organisation = contactTable.getValueAt(selectedRow, 3).toString();
				String emails = contactTable.getValueAt(selectedRow, 4).toString();
				String addresses = contactTable.getValueAt(selectedRow, 5).toString();
				String websites = contactTable.getValueAt(selectedRow, 6).toString();
				String im = contactTable.getValueAt(selectedRow, 7).toString();
				String skype = contactTable.getValueAt(selectedRow, 8).toString();
				String notes = contactTable.getValueAt(selectedRow, 9).toString();
				File f = new File(fc.getCurrentDirectory() + "/data/contact_" + id + ".jpg");
				if (f.isFile()) {
					contactInfo.setInfo(name, numbers, organisation, emails, addresses, websites,
							im, skype, notes, f);
				} else {
					contactInfo.setInfo(name, numbers, organisation, emails, addresses, websites,
							im, skype, notes);
				}
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			if (e.getSource().equals(browserHTable)) {
				openContextMenu(e, browserHTable);
			} else if (e.getSource().equals(browserSTable)) {
				openContextMenu(e, browserSTable);
			} else if (e.getSource().equals(calendarTable)) {
				openContextMenu(e, calendarTable);
			} else if (e.getSource().equals(callTable)) {
				openContextMenu(e, callTable);
			} else if (e.getSource().equals(contactTable)) {
				openContextMenu(e, contactTable);
			} else if (e.getSource().equals(mmsTable)) {
				openContextMenu(e, mmsTable);
			} else if (e.getSource().equals(smsTable)) {
				openContextMenu(e, smsTable);
			}
		}
	}

	private void openContextMenu(MouseEvent e, JTable table) {

		// selects the row which was right-clicked
		Point p = e.getPoint();
		int rowNumber = table.rowAtPoint(p);
		table.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);

		currentX = e.getX();
		currentY = e.getY();
		currentTable = table;

		contextMenu.show(table, currentX, currentY);

	}

	@Override
	public void keyPressed(KeyEvent e) {
		// not used
	}

	@Override
	public void keyReleased(KeyEvent e) {
		String search = "";
		LiveSearchTableModel model = null;
		if (e.getSource().equals(contactSearch)) {
			search = contactSearch.getText();
			model = (LiveSearchTableModel) contactTable.getModel();
		} else if (e.getSource().equals(browserHSearch)) {
			search = browserHSearch.getText();
			model = (LiveSearchTableModel) browserHTable.getModel();
		} else if (e.getSource().equals(browserSSearch)) {
			search = browserSSearch.getText();
			model = (LiveSearchTableModel) browserSTable.getModel();
		} else if (e.getSource().equals(callsSearch)) {
			search = callsSearch.getText();
			model = (LiveSearchTableModel) callTable.getModel();
		} else if (e.getSource().equals(calendarSearch)) {
			search = calendarSearch.getText();
			model = (LiveSearchTableModel) calendarTable.getModel();
		} else if (e.getSource().equals(smsSearch)) {
			search = smsSearch.getText();
			model = (LiveSearchTableModel) smsTable.getModel();
		} else if (e.getSource().equals(mmsSearch)) {
			search = mmsSearch.getText();
			model = (LiveSearchTableModel) mmsTable.getModel();
		}
		if (!search.equals("")) {
			model.filterData(search);
		} else {
			model.resetData();
		}
		browserHScrollPane.getVerticalScrollBar().setValue(0);
		browserSScrollPane.getVerticalScrollBar().setValue(0);
		callScrollPane.getVerticalScrollBar().setValue(0);
		calendarScrollPane.getVerticalScrollBar().setValue(0);
		contactScrollPane.getVerticalScrollBar().setValue(0);
		mmsScrollPane.getVerticalScrollBar().setValue(0);
		smsScrollPane.getVerticalScrollBar().setValue(0);

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// not used
	}

	@Override
	public void focusGained(FocusEvent e) {
		if (e.getSource().equals(contactSearch)) {
			contactSearch.setText("");
		} else if (e.getSource().equals(browserHSearch)) {
			browserHSearch.setText("");
		} else if (e.getSource().equals(browserSSearch)) {
			browserSSearch.setText("");
		} else if (e.getSource().equals(calendarSearch)) {
			calendarSearch.setText("");
		} else if (e.getSource().equals(callsSearch)) {
			callsSearch.setText("");
		} else if (e.getSource().equals(mmsSearch)) {
			mmsSearch.setText("");
		} else if (e.getSource().equals(smsSearch)) {
			smsSearch.setText("");
		}

	}

	@Override
	public void focusLost(FocusEvent e) {
		// if (e.getSource().equals(contactSearch)) {
		// contactSearch.setText("Search");
		// }
	}

	@Override
	public void setADBThread(ADBThread adb) {
		this.adb = adb;
	}

	@Override
	public void setCaseFolder(File caseFolder) {
		// plugin doesn't need to save something
	}

	@Override
	public void reactToADBResult(String result) {
		// TODO Auto-generated method stub
		
	}

}
