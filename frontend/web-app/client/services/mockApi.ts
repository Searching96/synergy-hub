export interface CustomerInfo {
  name: string;
  phone: string;
  email: string;
  address: string;
}

export interface TrackingEvent {
  timestamp: string;
  status: DeliveryStatus;
  message: string;
  location?: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  // Use unified DeliveryStatus throughout the codebase
  status: DeliveryStatus;
  targetDeliveryDate: string;
  codAmount: number;
  recipientName: string;
  recipientPhone: string;
  address: string;
  createdDate: string;
  items: string;
  assignedDriverId: string; // NEW: Track driver assignment
  trackingHistory?: TrackingEvent[];
}

// Add unified delivery status type and Delivery interface
export type DeliveryStatus =
  | "pickup_pending"   // newly added: waiting to be picked up
  | "received"         // in office, just received
  | "sorted"           // in office, sorted
  | "export_pending"   // waiting export / payment / dispatch
  | "in-transit"       // on the way
  | "out-for-delivery" // last-mile
  | "delivered"
  | "failed"
  | "returned"         // new: returned to sender
  | "cancelled";       // new: cancelled by user/system

export interface Delivery {
  id: string;
  orderNumber: string;
  customerName: string;
  customerPhone: string;
  address: string;
  status: DeliveryStatus;
  lat: number;
  lng: number;
  estimatedTime: string;
  codAmount: number;
  notes?: string;
  assignedDriverId: string; // newly added: which driver handles this delivery
}

// Mock customer data - in a real app this would come from a backend API
const mockCustomerInfo: CustomerInfo = {
  name: "Nguyễn Văn A",
  phone: "0901234567",
  email: "nguyenva@example.com",
  address: "Hàn Thuyên, Khu phố 6, P.Thủ Đức, TP.HCM"
};

// Helper function to generate realistic mock orders
function generateMockOrders(count: number): Order[] {
  const statuses: DeliveryStatus[] = [
    'pickup_pending', 'received', 'sorted', 'export_pending', 'in-transit', 'out-for-delivery', 'delivered', 'failed', 'returned', 'cancelled'
  ];
  const names = [
    'Nguyễn Văn A', 'Trần Thị B', 'Lê Văn C', 'Phạm Thị D', 'Hoàng Văn E', 'Võ Thị F', 'Đặng Văn G', 'Bùi Thị H', 'Đỗ Văn I', 'Ngô Thị J'
  ];
  const addresses = [
    '123 Đường Lê Lợi, Quận 1, TP.HCM', '456 Đường Nguyễn Huệ, Quận 1, TP.HCM', '789 Đường Trần Hưng Đạo, Quận 5, TP.HCM',
    '321 Đường Bà Triệu, Quận Hà Đông, Hà Nội', '654 Đường Hàng Bài, Quận Hoàn Kiếm, Hà Nội', '987 Đường Phan Đình Phùng, Quận Phú Nhuận, TP.HCM',
    '135 Đường Võ Văn Tần, Quận 3, TP.HCM', '246 Đường Cách Mạng Tháng 8, Quận 10, TP.HCM', '579 Đường Nguyễn Trãi, Quận 1, TP.HCM',
    '864 Đường Lý Thường Kiệt, Quận Hoàn Kiếm, Hà Nội'
  ];
  const items = [
    'Quần áo, Giày dép', 'Điện thoại', 'Sách, Dụng cụ học tập', 'Laptop', 'Mỹ phẩm', 'Đồ gia dụng', 'Thực phẩm', 'Đồ chơi', 'Văn phòng phẩm', 'Điện tử'
  ];

  const orders: Order[] = [];
  for (let i = 0; i < count; i++) {
    const status = statuses[Math.floor(Math.random() * statuses.length)];
    const createdDate = new Date(2024, 0, Math.floor(Math.random() * 30) + 1).toISOString().split('T')[0];
    const targetDeliveryDate = new Date(new Date(createdDate).getTime() + Math.floor(Math.random() * 7 + 1) * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    const codAmount = Math.random() > 0.3 ? Math.floor(Math.random() * 2000000) + 100000 : 0;

    // Generate tracking history based on status
    let trackingHistory: TrackingEvent[] = [];
    if (status === 'delivered') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
        { timestamp: `${targetDeliveryDate} 10:15`, status: 'out-for-delivery', message: 'Đơn hàng đang được giao', location: 'Điểm giao hàng' },
        { timestamp: `${targetDeliveryDate} 14:50`, status: 'delivered', message: 'Đơn hàng đã được giao thành công', location: addresses[i % addresses.length] },
      ];
    } else if (status === 'in-transit') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
      ];
    } else if (status === 'failed') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
        { timestamp: `${targetDeliveryDate} 10:15`, status: 'out-for-delivery', message: 'Đơn hàng đang được giao', location: 'Điểm giao hàng' },
        { timestamp: `${targetDeliveryDate} 14:50`, status: 'failed', message: 'Giao hàng thất bại - Khách không có nhà', location: addresses[i % addresses.length] },
      ];
    } else if (status === 'out-for-delivery') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
        { timestamp: `${targetDeliveryDate} 09:00`, status: 'out-for-delivery', message: 'Đơn hàng đang được giao', location: 'Điểm giao hàng' },
      ];
    } else if (status === 'sorted') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
      ];
    } else if (status === 'export_pending') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
      ];
    } else if (status === 'received') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
      ];
    } else if (status === 'pickup_pending') {
      trackingHistory = []; // No events yet
    } else if (status === 'returned') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
        { timestamp: `${targetDeliveryDate} 10:15`, status: 'out-for-delivery', message: 'Đơn hàng đang được giao', location: 'Điểm giao hàng' },
        { timestamp: `${targetDeliveryDate} 14:50`, status: 'failed', message: 'Đơn hàng được trả về người gửi', location: addresses[i % addresses.length] },
      ];
    } else if (status === 'cancelled') {
      trackingHistory = [
        { timestamp: `${createdDate} 08:30`, status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
        { timestamp: `${createdDate} 14:20`, status: 'cancelled', message: 'Đơn hàng đã bị hủy', location: 'TP.HCM' },
      ];
    }
    // For other statuses, trackingHistory remains empty

    // NEW: Assign driver - driver-001 gets ~10% of orders (every 11th order)
    const assignedDriverId = (i % 11 === 0) ? 'driver-001' : `driver-${(i % 5) + 2}`;

    orders.push({
      id: (i + 1).toString(),
      orderNumber: `VN${(123456789000 + i).toString().padStart(12, '0')}VN`,
      status,
      targetDeliveryDate,
      codAmount,
      recipientName: names[i % names.length],
      recipientPhone: `09${Math.floor(Math.random() * 90000000) + 10000000}`,
      address: addresses[i % addresses.length],
      createdDate,
      items: items[i % items.length],
      assignedDriverId, // NEW
      trackingHistory,
    });
  }
  return orders;
}

// Generate 50 mock orders for realism
const mockOrders: Order[] = generateMockOrders(50);

// Add sample orders with common order numbers for testing
const sampleOrders: Order[] = [
  {
    id: 'sample-1',
    orderNumber: 'VN123456789VN',
    status: 'delivered',
    targetDeliveryDate: '2024-01-20',
    codAmount: 500000,
    recipientName: 'Nguyễn Văn A',
    recipientPhone: '0901234567',
    address: '123 Đường Lê Lợi, Quận 1, TP.HCM',
    createdDate: '2024-01-15',
    items: 'Điện thoại',
    assignedDriverId: 'driver-001', // NEW: Assigned to our driver
    trackingHistory: [
      { timestamp: '2024-01-15 08:30', status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
      { timestamp: '2024-01-15 14:20', status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
      { timestamp: '2024-01-20 10:15', status: 'out-for-delivery', message: 'Đơn hàng đang được giao', location: 'Điểm giao hàng' },
      { timestamp: '2024-01-20 14:50', status: 'delivered', message: 'Đơn hàng đã được giao thành công', location: '123 Đường Lê Lợi, Quận 1, TP.HCM' },
    ],
  },
  {
    id: 'sample-2',
    orderNumber: 'VN987654321VN',
    status: 'in-transit',
    targetDeliveryDate: '2024-01-18',
    codAmount: 0,
    recipientName: 'Trần Thị B',
    recipientPhone: '0907654321',
    address: '456 Đường Nguyễn Huệ, Quận 1, TP.HCM',
    createdDate: '2024-01-14',
    items: 'Sách',
    assignedDriverId: 'driver-003', // NEW: Different driver
    trackingHistory: [
      { timestamp: '2024-01-14 08:30', status: 'received', message: 'Đơn hàng được lấy từ điểm gửi', location: 'TP.HCM' },
      { timestamp: '2024-01-14 14:20', status: 'in-transit', message: 'Đơn hàng đang được vận chuyển', location: 'Hệ thống phân loại' },
    ],
  },
];

// Combine generated and sample orders
const allMockOrders = [...mockOrders, ...sampleOrders];

// Mock API functions
export async function fetchCustomerInfo(): Promise<CustomerInfo> {
  // Simulate API call delay
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockCustomerInfo);
    }, 300);
  });
}

export async function fetchOrders(): Promise<Order[]> {
  // Simulate API call delay
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(allMockOrders);
    }, 300);
  });
}

// New request params for fetching deliveries
export interface DeliveryRequest {
  driverId?: string;
  statuses?: DeliveryStatus[]; // optional status filter
}

// New helper to fetch deliveries derived from orders (supports filtering)
export async function fetchDeliveries(params?: DeliveryRequest): Promise<Delivery[]> {
  return new Promise((resolve) => {
    setTimeout(() => {
      // Create deliveries with an assignedDriverId for filtering
      const deliveries: Delivery[] = allMockOrders.map((o, i) => {
        // Deterministic assignment: roughly 4-5 deliveries assigned to driver-001 (i % 11 === 0)
        const assignedDriverId = (i % 11 === 0) ? 'driver-001' : `driver-${(i % 5) + 2}`;
        return {
          id: o.id,
          orderNumber: o.orderNumber,
          customerName: o.recipientName,
          customerPhone: o.recipientPhone,
          address: o.address,
          status: o.status as DeliveryStatus,
          lat: 10.7769 + (i - 1) * 0.002,
          lng: 106.7009 + (i - 1) * -0.003,
          estimatedTime: o.targetDeliveryDate,
          codAmount: o.codAmount,
          notes: o.items,
          assignedDriverId,
        };
      });

      let result = deliveries;

      // Filter by driverId if provided
      if (params?.driverId) {
        result = result.filter(d => d.assignedDriverId === params.driverId);
      }

      // Optional: filter by statuses if provided
      if (params?.statuses && params.statuses.length > 0) {
        result = result.filter(d => params.statuses!.includes(d.status));
      }

      resolve(result);
    }, 300);
  });
}

export function getStatusLabel(status: DeliveryStatus): string {
  const statusLabels: Record<DeliveryStatus, string> = {
    pickup_pending: "Chờ lấy",
    received: "Đã nhận",
    sorted: "Đã phân loại",
    export_pending: "Đợi xuất",
    "in-transit": "Đang vận chuyển",
    "out-for-delivery": "Đang giao",
    delivered: "Đã giao",
    failed: "Giao thất bại",
    returned: "Đã hoàn trả",
    cancelled: "Đã hủy",
  };
  return statusLabels[status] ?? status;
}

export function getStatusColor(status: DeliveryStatus): string {
  const colors: Record<DeliveryStatus, string> = {
    pickup_pending: "text-yellow-600",
    received: "text-yellow-600",
    sorted: "text-purple-600",
    export_pending: "text-amber-600",
    "in-transit": "text-blue-600",
    "out-for-delivery": "text-blue-600",
    delivered: "text-green-600",
    failed: "text-red-600",
    returned: "text-gray-600",
    cancelled: "text-gray-600",
  };
  return colors[status] ?? "text-muted-foreground";
}

export function getStatusBgColor(status: DeliveryStatus): string {
  const colors: Record<DeliveryStatus, string> = {
    pickup_pending: "bg-yellow-50",
    received: "bg-yellow-50",
    sorted: "bg-purple-50",
    export_pending: "bg-amber-50",
    "in-transit": "bg-blue-50",
    "out-for-delivery": "bg-blue-50",
    delivered: "bg-green-50",
    failed: "bg-red-50",
    returned: "bg-gray-50",
    cancelled: "bg-gray-50",
  };
  return colors[status] ?? "bg-muted/10";
}

export async function fetchOrderTracking(orderNumber: string): Promise<{
  order: Order;
  trackingHistory: TrackingEvent[];
  currentMilestoneIndex: number;
}> {
  // Simulate API call delay
  return new Promise((resolve) => {
    setTimeout(() => {
      const order = allMockOrders.find((o) => o.orderNumber === orderNumber);
      if (!order) {
        throw new Error("Order not found");
      }

      const trackingHistory = order.trackingHistory || [];
      const milestones = ["received", "in-transit", "out-for-delivery", "delivered"] as const;
      
      // Find the highest milestone index reached
      let currentMilestoneIndex = -1;
      for (const event of trackingHistory) {
        const index = milestones.indexOf(event.status as any);
        if (index !== -1) {
          currentMilestoneIndex = Math.max(currentMilestoneIndex, index);
        }
      }

      resolve({
        order,
        trackingHistory,
        currentMilestoneIndex,
      });
    }, 300);
  });
}

export function formatDateTime(dateTimeString: string): string {
  try {
    const [datePart, timePart] = dateTimeString.split(" ");
    const [year, month, day] = datePart.split("-");
    return `${timePart} ${day}/${month}/${year}`;
  } catch {
    return dateTimeString;
  }
}

// --- Entities for Postal Worker ---

// Sorting Bins
export interface SortingBin {
  id: string;
  route: string;
  district: string;
  count: number;
  color: string;
  containerCode: string;
  capacity: number;  // Maximum packages it can hold
  currentLoad: number;  // Current number of packages
  location: string;  // Physical location in the facility
  status: 'active' | 'maintenance' | 'full';  // Operational status
  assignedDriverId?: string; // NEW: Driver responsible for this route
}

const mockSortingBins: SortingBin[] = [
  { 
    id: 'route-a', 
    route: 'Tuyến A', 
    district: 'Quận 1, Quận 3, Quận 4, Quận 5',
    count: 15, 
    color: 'bg-blue-100 text-blue-800', 
    containerCode: 'CNT-A001',
    capacity: 50,
    currentLoad: 15,
    location: 'Zone A - Bay 1',
    status: 'active',
    assignedDriverId: 'driver-001' // Nguyễn Văn A
  },
  { 
    id: 'route-b', 
    route: 'Tuyến B', 
    district: 'Quận 2, Quận 7, Quận 9, Quận 10, Thủ Đức',
    count: 8, 
    color: 'bg-green-100 text-green-800', 
    containerCode: 'CNT-B002',
    capacity: 40,
    currentLoad: 8,
    location: 'Zone B - Bay 2',
    status: 'active',
    assignedDriverId: 'driver-002'
  },
  { 
    id: 'route-c', 
    route: 'Tuyến C', 
    district: 'Quận 6, Quận 8, Quận 11, Quận 12, Bình Tân',  // Southern districts
    count: 12, 
    color: 'bg-orange-100 text-orange-800', 
    containerCode: 'CNT-C003',
    capacity: 45,
    currentLoad: 12,
    location: 'Zone C - Bay 3',
    status: 'active'
  },
  { 
    id: 'route-d', 
    route: 'Tuyến D', 
    district: 'Quận Phú Nhuận, Quận Bình Thạnh, Quận Tân Bình',  // Additional central-south districts
    count: 10, 
    color: 'bg-purple-100 text-purple-800', 
    containerCode: 'CNT-D004',
    capacity: 35,
    currentLoad: 10,
    location: 'Zone D - Bay 4',
    status: 'active'
  },
  { 
    id: 'express', 
    route: 'Hỏa tốc', 
    district: 'Hỏa tốc',  // Dedicated for express services
    count: 2, 
    color: 'bg-red-100 text-red-800', 
    containerCode: 'CNT-E005',
    capacity: 15,
    currentLoad: 2,
    location: 'Zone Express - Bay 5',
    status: 'active'
  },
  { 
    id: 'international', 
    route: 'Quốc tế', 
    district: 'Quốc tế',  // Dedicated for international services
    count: 1, 
    color: 'bg-indigo-100 text-indigo-800', 
    containerCode: 'CNT-I006',
    capacity: 10,
    currentLoad: 1,
    location: 'Zone International - Bay 6',
    status: 'active'
  },
];

export async function fetchSortingBins(): Promise<SortingBin[]> {
  return new Promise(resolve => setTimeout(() => resolve(mockSortingBins), 300));
}

// Dispatch Batches
export interface DispatchBatch {
  id: string;
  route: string;
  driver: string; // driver name
  driverId?: string; // NEW: driver identifier
  packageCount: number;
  status: 'ready' | 'loading' | 'dispatched';
  estimatedTime: string;
}

const mockDispatchBatches: DispatchBatch[] = [
  { id: '1', route: 'Tuyến A - Q1, Q3, Q4, Q5', driver: 'Nguyễn Văn A', driverId: 'driver-001', packageCount: 15, status: 'ready', estimatedTime: '14:00' },
  { id: '2', route: 'Tuyến B - Q2, Q7, Q9, Q10, Thủ Đức', driver: 'Trần Văn B', driverId: 'driver-002', packageCount: 8, status: 'loading', estimatedTime: '14:30' },
  { id: '3', route: 'Tuyến C - Q6, Q8, Q11, Q12, Bình Tân', driver: 'Lê Văn C', driverId: 'driver-003', packageCount: 12, status: 'ready', estimatedTime: '15:00' },
];

export async function fetchDispatchBatches(): Promise<DispatchBatch[]> {
  return new Promise(resolve => setTimeout(() => resolve(mockDispatchBatches), 300));
}

// Stats for Postal Worker Dashboard
export interface PostalWorkerStats {
  pendingIngest: number;
  sorted: number;
  pendingDispatch: number;
  completedToday: number;
}

export async function fetchPostalWorkerStats(driverId?: string): Promise<PostalWorkerStats> {
  return new Promise(resolve => {
    setTimeout(() => {
      const filtered = allMockOrders.filter(o => !driverId || o.assignedDriverId === driverId);
      const stats: PostalWorkerStats = {
        pendingIngest: filtered.filter(o => o.status === 'pickup_pending').length,
        sorted: filtered.filter(o => o.status === 'sorted').length,
        pendingDispatch: filtered.filter(o => o.status === 'export_pending').length,
        completedToday: filtered.filter(o => o.status === 'delivered' && o.targetDeliveryDate.startsWith('2024-01-15')).length,
      };
      resolve(stats);
    }, 300);
  });
}

// Package Info for Scanning (used in sorting, dispatch, etc.)
export interface PackageInfoForScan {
  orderNumber: string;
  destination: string;
  recommendedRoute: string;  // Changed from recommendedRouteId to recommendedRoute for display
}

const getRouteIdFromAddress = (address: string): string => {
  // Route A: Quận 1, 3, 4, 5
  if (address.includes('Q1') || address.includes('Q3') || address.includes('Q4') || address.includes('Q5') ||
      address.includes('Quận 1') || address.includes('Quận 3') || address.includes('Quận 4') || address.includes('Quận 5')) return 'route-a';
  // Route B: Quận 2, 7, 9, 10, Thủ Đức
  if (address.includes('Q2') || address.includes('Q7') || address.includes('Q9') || address.includes('Q10') ||
      address.includes('Quận 2') || address.includes('Quận 7') || address.includes('Quận 9') || address.includes('Quận 10') ||
      address.includes('Thủ Đức')) return 'route-b';
  // Route C: Quận 6, 8, 11, 12, Bình Tân
  if (address.includes('Q6') || address.includes('Q8') || address.includes('Q11') || address.includes('Q12') ||
      address.includes('Quận 6') || address.includes('Quận 8') || address.includes('Quận 11') || address.includes('Quận 12') ||
      address.includes('Bình Tân')) return 'route-c';
  // Route D: Quận Phú Nhuận, Bình Thạnh, Tân Bình
  if (address.includes('Phú Nhuận') || address.includes('Bình Thạnh') || address.includes('Tân Bình')) return 'route-d';
  // Default to route-d for any unmatched (including COD, express, international if not specified)
  return 'route-d';
};

export async function fetchPackageInfoForScan(): Promise<PackageInfoForScan> {
  return new Promise(resolve => {
    setTimeout(() => {
      const randomOrder = allMockOrders[Math.floor(Math.random() * allMockOrders.length)];
      const routeId = getRouteIdFromAddress(randomOrder.address);
      const routeName = mockSortingBins.find(b => b.id === routeId)?.route || routeId;  // Map ID to name
      resolve({
        orderNumber: randomOrder.orderNumber,
        destination: randomOrder.address,
        recommendedRoute: routeName,  // Use route name for display
      });
    }, 500);
  });
}

// Ingested Packages for Postal Worker Ingest Page
export interface IngestedPackage {
  id: string;
  orderNumber: string;
  origin: string;
  weight: number;
  timestamp: string;
}

const mockIngestedPackages: IngestedPackage[] = [
  {
    id: '1',
    orderNumber: 'VN123456789VN',
    origin: 'Bưu cục A',
    weight: 1200,
    timestamp: '10:30'
  },
  {
    id: '2',
    orderNumber: 'VN987654321VN',
    origin: 'Bưu cục B',
    weight: 800,
    timestamp: '09:45'
  },
  // Add more mock data as needed
];

export async function fetchIngestedPackages(): Promise<IngestedPackage[]> {
  return new Promise(resolve => setTimeout(() => resolve(mockIngestedPackages), 300));
}

export async function addIngestedPackage(packageData: Omit<IngestedPackage, 'id' | 'timestamp'>): Promise<IngestedPackage> {
  return new Promise(resolve => {
    setTimeout(() => {
      const newPackage: IngestedPackage = {
        id: Date.now().toString(),
        ...packageData,
        timestamp: new Date().toLocaleTimeString('vi-VN')
      };
      mockIngestedPackages.unshift(newPackage); // Add to beginning for UI
      resolve(newPackage);
    }, 500);
  });
}
