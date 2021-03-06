package Routing;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

import org.jnetpcap.PcapIf;

import Routing.ARPLayer._ARPCache_Entry;
import Routing.ARPLayer._Proxy_Entry;

public class RoutingDlg extends JFrame implements BaseLayer {
	public int nUpperLayerCount = 0;
	public String pLayerName = null;
	public BaseLayer p_UnderLayer = null;
	public ArrayList<BaseLayer> p_aUpperLayer = new ArrayList<BaseLayer>();
	private static LayerManager m_LayerMgr = new LayerManager();
	
	static Hashtable<String, _ARPCache_Entry> _ARPCache_Table;
	static Hashtable<String, _Proxy_Entry> _Proxy_Table;
	
	public static ArrayList<String> portIpList = new ArrayList<>();
	public static ArrayList<String> portMacList = new ArrayList<>();
	
	DefaultTableModel RoutingTableModel;
	Vector<String> RoutingTableColumns = new Vector<String>();
	Vector<String> RoutingTableRows = new Vector<String>();
	
	static DefaultTableModel ARPTableModel;
	static Vector<String> ARPTableColumns = new Vector<String>();
	static Vector<String> ARPTableRows = new Vector<String>();
	
	DefaultTableModel ProxyTableModel;
	Vector<String> ProxyTableColumns = new Vector<String>();
	Vector<String> ProxyTableRows = new Vector<String>();
	
	JTable StaticRoutingTable;
	JTable ARPCacheTable;
	JTable ProxyTable;
	
	// Main Interface
	Container Main_contentPane;
	JButton Main_RoutingTableAddButton;
	JButton Main_RoutingTableDeleteButton;
	JButton Main_ARPDeleteButton;
	JButton Main_ProxyAddButton;
	JButton Main_ProxyDeleteButton;
	
	// Route Add Interface
	JFrame Route_RouteAddFrame;
	Container Route_ContentPane;
	JTextField Route_DstField;
	JTextField Route_NetmaskField;
	JTextField Route_GatewayField;
	JCheckBox Route_UpCheckBox;
	JCheckBox Route_GatewayCheckBox;
	JCheckBox Route_HostCheckBox;
	JComboBox<String> Route_InterfaceComboBox;
	JButton Route_AddButton;
	JButton Route_CancelButton;
	
	// Proxy Add Interface
	JFrame Proxy_ProxyAddFrame;
	Container Proxy_ContentPane;
	JTextField Proxy_IpAddressField;
	JTextField Proxy_MacAddressField;
	JComboBox<String> Proxy_InterfaceComboBox;
	JButton Proxy_AddButton;
	JButton Proxy_CancelButton;
		
	public static void main(String[] args) throws SocketException {
		m_LayerMgr.AddLayer(new RoutingDlg("GUI"));
		m_LayerMgr.AddLayer(new IPLayer("IP"));
		m_LayerMgr.AddLayer(new ARPLayer("ARP"));
		m_LayerMgr.AddLayer(new EthernetLayer("ETHERNET"));
		m_LayerMgr.AddLayer(new NILayer("NI"));
		
		m_LayerMgr.ConnectLayers(" NI ( *ETHERNET ( *ARP +IP ( *GUI ) ) )");
		m_LayerMgr.GetLayer("IP").SetUnderLayer(m_LayerMgr.GetLayer("ARP"));
		initAddress();
		// NILayer AdapterNumber 설정
		((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(0);
		((NILayer) m_LayerMgr.GetLayer("NI")).SetAdapterNumber(1);
	}
	
	// 각 Layer에서 보유해야할 Ip와 Mac Address를 활성화하는 함수
	public static void initAddress() {
		((IPLayer) m_LayerMgr.GetLayer("IP")).initAddress();
		((ARPLayer) m_LayerMgr.GetLayer("ARP")).initAddress();
		((EthernetLayer) m_LayerMgr.GetLayer("ETHERNET")).initAddress();
		String port0_ip = NILayer.getIpAddress(0);
		String port0_mac = NILayer.getMacAddress(0);
		String port1_ip = NILayer.getIpAddress(1);
		String port1_mac = NILayer.getMacAddress(1);
		portIpList.add(port0_ip);
		portIpList.add(port1_ip);
		portMacList.add(port0_mac);
		portMacList.add(port1_mac);
	}
	
	// Proxy Table Gui에 새로운 Row를 추가하는 함수
	public void addProxyTableRow(String[] value) {
		ProxyTableRows = new Vector<String>();
		ProxyTableRows.addElement(value[0]);
		ProxyTableRows.addElement(value[1]);
		ProxyTableRows.addElement(value[2]);
		ProxyTableModel.addRow(ProxyTableRows);
	}
	
	// Routing Table GUI에 새로운 Row를 추가하는 함수
	public synchronized void addRoutingTableRow(String[] input) {
		RoutingTableRows = new Vector<String>();
		RoutingTableRows.addElement(input[0]);
		RoutingTableRows.addElement(input[1]);
		RoutingTableRows.addElement(input[2]);
		RoutingTableRows.addElement(input[3]);
		RoutingTableRows.addElement(input[4]);
		RoutingTableRows.addElement(input[5]);
		RoutingTableModel.addRow(RoutingTableRows);
	}
	
	// ARP GUI Table을 업데이트하는 함수
	public synchronized static void addArpCacheToTable(String targetKey, _ARPCache_Entry entry) {
		int rowCount = ARPTableModel.getRowCount();
		boolean find = false;
		int saveidx = 0;
		// 현재 Table에 해당하는 Key가 존재하면 find를 True로
		for (int idx = 0; idx < rowCount; idx++) {
			String ipKey = (String) ARPTableModel.getValueAt(idx, 0);
			if(ipKey.equals(targetKey)) {
				saveidx = idx;
				find = true;
				break;
			}
		}
		if(find) {
			// find가 True면 해당 내용을 업데이트한다
			String macAddress = Translator.macToString(entry.addr);
			String flag = entry.status;
			ARPTableModel.setValueAt(macAddress, saveidx, 1);
			ARPTableModel.setValueAt(flag, saveidx, 3);
		}
		else {
			// find가 False면 새로운 Row를 입력한다.
			if(entry.status.equals("Complete")){
				ARPTableRows = new Vector<String>();
				ARPTableRows.addElement(targetKey);
				ARPTableRows.addElement(Translator.macToString(entry.addr));
				ARPTableRows.addElement(entry.arp_interface);
				ARPTableRows.addElement(entry.status);
				ARPTableModel.addRow(ARPTableRows);
			} else {
				ARPTableRows = new Vector<String>();
				ARPTableRows.addElement(targetKey);
				ARPTableRows.addElement("??:??:??:??:??:??");
				ARPTableRows.addElement(entry.arp_interface);
				ARPTableRows.addElement(entry.status);
				ARPTableModel.addRow(ARPTableRows);
			}
			
		}
	}
	
	// 각 버튼 Event 설정하는 EventListener
	class buttonEventListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == Main_RoutingTableAddButton) {
				// Route Add Frame 호출
				RouteAddFrame();
			}
			if (e.getSource() == Main_RoutingTableDeleteButton) {
				// Route Delete
				int targetIdx = StaticRoutingTable.getSelectedRow();
				String targetKey = (String) RoutingTableModel.getValueAt(targetIdx, 0);
				RoutingTableModel.removeRow(targetIdx);
				((IPLayer) m_LayerMgr.GetLayer("IP")).removeEntryFromRoutingTable(targetKey);
			}
			if (e.getSource() == Main_ARPDeleteButton) {
				// ARP Delete
				int targetIdx = ARPCacheTable.getSelectedRow();
				String targetKey = (String) ARPTableModel.getValueAt(targetIdx, 0);
				ARPTableModel.removeRow(targetIdx);
				_ARPCache_Table.remove(targetKey);
			}
			if (e.getSource() == Main_ProxyAddButton) {
				// Proxy Add
				ProxyAddFrame();
			}
			if (e.getSource() == Main_ProxyDeleteButton) {
				// Proxy Delete
				int targetIdx = ProxyTable.getSelectedRow();
				String targetKey = (String) ProxyTableModel.getValueAt(targetIdx, 0);
				ProxyTableModel.removeRow(targetIdx);
				_Proxy_Table.remove(targetKey);
			}
			if (e.getSource() == Route_AddButton) {
				// Route Add -> Routing Table에 입력
				String[] input = new String[6];
				input[0] = Route_DstField.getText();
				input[1] = Route_NetmaskField.getText();
				input[2] = Route_GatewayField.getText();
				input[3] = "";
				if(Route_UpCheckBox.isSelected()) { input[3] += "U"; }
				if(Route_GatewayCheckBox.isSelected()) { input[3] += "G"; }
				if(Route_HostCheckBox.isSelected()) { input[3] += "H"; }
				input[4] = Integer.toString(Route_InterfaceComboBox.getSelectedIndex());
				input[5] = "1";
				((IPLayer) m_LayerMgr.GetLayer("IP")).addToRoutingTable(input);
				addRoutingTableRow(input);
				Route_RouteAddFrame.dispose();
			}
			if (e.getSource() == Route_CancelButton) {
				// Route Add Cancel
				Route_RouteAddFrame.dispose();
			}
			if (e.getSource() == Proxy_AddButton) {
				// Proxy 동작 x
			}
			if (e.getSource() == Proxy_CancelButton) {
				Proxy_ProxyAddFrame.dispose();
			}
		}
	}
		
	public RoutingDlg(String pName) throws SocketException {
		pLayerName = pName;
		RoutingTableColumns.addElement("Destination");
		RoutingTableColumns.addElement("NetMask");
		RoutingTableColumns.addElement("Gateway");
		RoutingTableColumns.addElement("Flag");
		RoutingTableColumns.addElement("Interface");
		RoutingTableColumns.addElement("Metric");
		
		ARPTableColumns.addElement("IP Address");
		ARPTableColumns.addElement("Ethernet Address");
		ARPTableColumns.addElement("Interface");
		ARPTableColumns.addElement("Flag");
		
		ProxyTableColumns.addElement("IP Address");
		ProxyTableColumns.addElement("Ehternet Address");
		ProxyTableColumns.addElement("Interface");
		
		RoutingTableModel = new DefaultTableModel(RoutingTableColumns, 0);
		ARPTableModel = new DefaultTableModel(ARPTableColumns, 0);
		ProxyTableModel = new DefaultTableModel(ProxyTableColumns, 0);
		
		setTitle("Static Routing Table");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(250, 250, 1100, 500);
		Main_contentPane = new JPanel();
		((JComponent) Main_contentPane).setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(Main_contentPane);
		Main_contentPane.setLayout(null);
		pLayerName = pName;
		
		JPanel staticRoutingPanel = new JPanel();
		staticRoutingPanel.setBorder(new TitledBorder(
				UIManager.getBorder("TitledBorder.border"), "Static Routing Table",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		staticRoutingPanel.setBounds(10, 5, 600, 425);
		Main_contentPane.add(staticRoutingPanel);
		staticRoutingPanel.setLayout(null);

		StaticRoutingTable = new JTable(RoutingTableModel);
		StaticRoutingTable.setBounds(0, 0, 580, 365);
		StaticRoutingTable.setShowGrid(false);

		JScrollPane RoutingTableScrollPane = new JScrollPane(StaticRoutingTable);
		RoutingTableScrollPane.setBounds(10, 15, 580, 365);
		staticRoutingPanel.add(RoutingTableScrollPane);
		
		Main_RoutingTableAddButton = new JButton("Add");
		Main_RoutingTableAddButton.setBounds(210, 385, 80, 30);
		Main_RoutingTableAddButton.addActionListener(new buttonEventListener());
		staticRoutingPanel.add(Main_RoutingTableAddButton);

		Main_RoutingTableDeleteButton = new JButton("Delete");
		Main_RoutingTableDeleteButton.setBounds(310, 385, 80, 30);
		Main_RoutingTableDeleteButton.addActionListener(new buttonEventListener());
		staticRoutingPanel.add(Main_RoutingTableDeleteButton);
		
		JPanel ARPCachePanel = new JPanel();
		ARPCachePanel.setBorder(new TitledBorder(
				UIManager.getBorder("TitledBorder.border"), "ARP Cache Table",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		ARPCachePanel.setBounds(620, 5, 450, 210);
		Main_contentPane.add(ARPCachePanel);
		ARPCachePanel.setLayout(null);

		ARPCacheTable = new JTable(ARPTableModel);
		ARPCacheTable.setBounds(0, 0, 580, 355);
		ARPCacheTable.setShowGrid(false);

		JScrollPane ARPTableScrollPane = new JScrollPane(ARPCacheTable);
		ARPTableScrollPane.setBounds(10, 15, 430, 150);
		ARPCachePanel.add(ARPTableScrollPane);
		
		Main_ARPDeleteButton = new JButton("Delete");
		Main_ARPDeleteButton.setBounds(190, 170, 80, 30);
		Main_ARPDeleteButton.addActionListener(new buttonEventListener());
		ARPCachePanel.add(Main_ARPDeleteButton);
		
		// ProxyARP Table
		JPanel ProxyARPPanel = new JPanel();
		ProxyARPPanel.setBorder(new TitledBorder(
				UIManager.getBorder("TitledBorder.border"), "Proxy ARP Table",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		ProxyARPPanel.setBounds(620, 220, 450, 210);
		Main_contentPane.add(ProxyARPPanel);
		ProxyARPPanel.setLayout(null);
		
		ProxyTable = new JTable(ProxyTableModel);
		ProxyTable.setBounds(0, 0, 580, 355);
		ProxyTable.setShowGrid(false);

		JScrollPane ProxyTableScrollPane = new JScrollPane(ProxyTable);
		ProxyTableScrollPane.setBounds(10, 15, 430, 150);
		ProxyARPPanel.add(ProxyTableScrollPane);
		
		Main_ProxyAddButton = new JButton("Add");
		Main_ProxyAddButton.setBounds(140, 170, 80, 30);
		Main_ProxyAddButton.addActionListener(new buttonEventListener());
		ProxyARPPanel.add(Main_ProxyAddButton);
		
		Main_ProxyDeleteButton = new JButton("Delete");
		Main_ProxyDeleteButton.setBounds(240, 170, 80, 30);
		Main_ProxyDeleteButton.addActionListener(new buttonEventListener());
		ProxyARPPanel.add(Main_ProxyDeleteButton);

		setVisible(true);
	}
	
	public void RouteAddFrame() {
		Route_RouteAddFrame = new JFrame("Static Route Add");
		Route_RouteAddFrame.setBounds(250, 250, 400, 300);
		Route_ContentPane = Route_RouteAddFrame.getContentPane();
		Route_RouteAddFrame.setLayout(null);
		Route_RouteAddFrame.setVisible(true);
			
		JLabel Route_DstLabel = new JLabel("Destination");
		Route_DstLabel.setBounds(20, 25, 100, 30);
		Route_ContentPane.add(Route_DstLabel);
			
		Route_DstField = new JTextField();
		Route_DstField.setBounds(130, 25, 230, 30);
		Route_ContentPane.add(Route_DstField);
			
		JLabel Route_NetmaskLabel = new JLabel("NetMask");
		Route_NetmaskLabel.setBounds(20, 60, 100, 30);
		Route_ContentPane.add(Route_NetmaskLabel);
			
		Route_NetmaskField = new JTextField();
		Route_NetmaskField.setBounds(130, 60, 230, 30);
		Route_ContentPane.add(Route_NetmaskField);
			
		JLabel Route_GatewayLabel = new JLabel("Gateway");
		Route_GatewayLabel.setBounds(20, 95, 100, 30);
		Route_ContentPane.add(Route_GatewayLabel);
			
		Route_GatewayField = new JTextField();
		Route_GatewayField.setBounds(130, 95, 230, 30);
		Route_ContentPane.add(Route_GatewayField);
			
		JLabel Route_FlagLabel = new JLabel("Flag");
		Route_FlagLabel.setBounds(20, 130, 100, 30);
		Route_ContentPane.add(Route_FlagLabel);
			
		Route_UpCheckBox = new JCheckBox("UP", false);
		Route_UpCheckBox.setBounds(130, 130, 45, 30);
		Route_ContentPane.add(Route_UpCheckBox);
			
		Route_GatewayCheckBox = new JCheckBox("Gateway", false);
		Route_GatewayCheckBox.setBounds(180, 130, 75, 30);
		Route_ContentPane.add(Route_GatewayCheckBox);
			
		Route_HostCheckBox = new JCheckBox("Host", false);
		Route_HostCheckBox.setBounds(260, 130, 55, 30);
		Route_ContentPane.add(Route_HostCheckBox);
			
		JLabel Route_InterfaceLabel = new JLabel("Interface");
		Route_InterfaceLabel.setBounds(20, 165, 100, 30);
		Route_ContentPane.add(Route_InterfaceLabel);
		Route_InterfaceComboBox = new JComboBox<>();
		
		List<PcapIf> l = ((NILayer) m_LayerMgr.GetLayer("NI")).m_pAdapterList;
		for (int i = 0; i < l.size(); i++)
			Route_InterfaceComboBox.addItem(l.get(i).getDescription() + " : " + l.get(i).getName());
			
		Route_InterfaceComboBox.setBounds(130, 165, 230, 30);
		Route_InterfaceComboBox.addActionListener(new buttonEventListener());
		Route_ContentPane.add(Route_InterfaceComboBox);// src address
		
		Route_AddButton = new JButton("Add");
		Route_AddButton.setBounds(120, 210, 80, 30);
		Route_AddButton.addActionListener(new buttonEventListener());
		Route_ContentPane.add(Route_AddButton);
		
		Route_CancelButton = new JButton("Cancel");
		Route_CancelButton.setBounds(210, 210, 80, 30);
		Route_CancelButton.addActionListener(new buttonEventListener());
		Route_ContentPane.add(Route_CancelButton);
			
	}
	
	public void ProxyAddFrame() {
		Proxy_ProxyAddFrame = new JFrame("Proxy ARP Add");
		Proxy_ProxyAddFrame.setBounds(250, 250, 400, 300);
		Proxy_ContentPane = Proxy_ProxyAddFrame.getContentPane();
		Proxy_ProxyAddFrame.setLayout(null);
		Proxy_ProxyAddFrame.setVisible(true);
			
		JLabel Proxy_DstLabel = new JLabel("IP");
		Proxy_DstLabel.setBounds(20, 25, 100, 30);
		Proxy_ContentPane.add(Proxy_DstLabel);
		Proxy_IpAddressField = new JTextField();
		Proxy_IpAddressField.setBounds(130, 25, 230, 30);
		Proxy_ContentPane.add(Proxy_IpAddressField);		
			
		JLabel Proxy_MacAddressLabel = new JLabel("MAC");
		Proxy_MacAddressLabel.setBounds(20, 60, 100, 30);
		Proxy_ContentPane.add(Proxy_MacAddressLabel);
		Proxy_MacAddressField = new JTextField();
		Proxy_MacAddressField.setBounds(130, 60, 230, 30);
		Proxy_ContentPane.add(Proxy_MacAddressField);
		
		JLabel Proxy_InterfaceLabel = new JLabel("Interface");
		Proxy_InterfaceLabel.setBounds(20, 95, 100, 30);
		Proxy_ContentPane.add(Proxy_InterfaceLabel);
		Proxy_InterfaceComboBox = new JComboBox<>();
		Proxy_InterfaceComboBox.setBounds(130, 95, 230, 30);
		Proxy_InterfaceComboBox.addActionListener(new buttonEventListener());
		Proxy_ContentPane.add(Proxy_InterfaceComboBox);// src address
		
		Proxy_AddButton = new JButton("Add");
		Proxy_AddButton.setBounds(120, 210, 80, 30);
		Proxy_AddButton.addActionListener(new buttonEventListener());
		Proxy_ContentPane.add(Proxy_AddButton);
		
		Proxy_CancelButton = new JButton("Cancel");
		Proxy_CancelButton.setBounds(210, 210, 80, 30);
		Proxy_CancelButton.addActionListener(new buttonEventListener());
		Proxy_ContentPane.add(Proxy_CancelButton);
	}
	
	@Override
	public void SetUnderLayer(BaseLayer pUnderLayer) {
		if (pUnderLayer == null)
			return;
		this.p_UnderLayer = pUnderLayer;
	}

	@Override
	public void SetUpperLayer(BaseLayer pUpperLayer) {
		if (pUpperLayer == null)
			return;
		this.p_aUpperLayer.add(nUpperLayerCount++, pUpperLayer);
		// nUpperLayerCount++;
	}

	@Override
	public String GetLayerName() {
		return pLayerName;
	}

	@Override
	public BaseLayer GetUnderLayer() {
		if (p_UnderLayer == null)
			return null;
		return p_UnderLayer;
	}

	@Override
	public BaseLayer GetUpperLayer(int nindex) {
		if (nindex < 0 || nindex > nUpperLayerCount || nUpperLayerCount < 0)
			return null;
		return p_aUpperLayer.get(nindex);
	}

	@Override
	public void SetUpperUnderLayer(BaseLayer pUULayer) {
		this.SetUpperLayer(pUULayer);
		pUULayer.SetUnderLayer(this);

	}
}
