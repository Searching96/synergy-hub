import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Truck, ShoppingCart, Package } from "lucide-react";

export default function Home() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-background to-muted/30 flex items-center justify-center p-4">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold mb-2">PostalFlow</h1>
          <p className="text-muted-foreground">Chọn vai trò của bạn</p>
        </div>

        <div className="space-y-4">
          <Link to="/customer/home" className="block">
            <Button variant="secondary" className="w-full h-32 rounded-xl flex flex-col items-center justify-center gap-3 shadow-sm hover:shadow-md transition-shadow">
              <ShoppingCart className="h-8 w-8" />
              <div className="flex flex-col items-center gap-1">
                <span className="text-base font-semibold">Khách hàng</span>
                <span className="text-xs text-muted-foreground">Gửi yêu cầu và khiếu nại</span>
              </div>
            </Button>
          </Link>

          <Link to="/delivery-driver/home" className="block">
            <Button variant="secondary" className="w-full h-32 rounded-xl flex flex-col items-center justify-center gap-3 shadow-sm hover:shadow-md transition-shadow">
              <Truck className="h-8 w-8" />
              <div className="flex flex-col items-center gap-1">
                <span className="text-base font-semibold">Bưu tá giao hàng</span>
                <span className="text-xs text-muted-foreground">Quản lý giao hàng</span>
              </div>
            </Button>
          </Link>

          <Link to="/postal-worker/home" className="block">
            <Button variant="secondary" className="w-full h-32 rounded-xl flex flex-col items-center justify-center gap-3 shadow-sm hover:shadow-md transition-shadow">
              <Package className="h-8 w-8" />
              <div className="flex flex-col items-center gap-1">
                <span className="text-base font-semibold">Nhân viên bưu điện</span>
                <span className="text-xs text-muted-foreground">Nhận và phân loại hàng</span>
              </div>
            </Button>
          </Link>
        </div>

        <div className="mt-8 p-4 rounded-lg bg-muted/50">
          <p className="text-xs text-muted-foreground text-center">
            Đây là chế độ demo cho phép bạn khám phá tất cả các quy trình
          </p>
        </div>
      </div>
    </div>
  );
}
