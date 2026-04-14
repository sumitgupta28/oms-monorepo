import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { orderApi, Order } from "../../api/orderApi";

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string,string> = {
    PENDING:"bg-yellow-50 text-yellow-700", VALIDATED:"bg-blue-50 text-blue-700",
    PAID:"bg-green-50 text-green-700", SHIPPED:"bg-teal-50 text-teal-700",
    DELIVERED:"bg-gray-50 text-gray-700", CANCELLED:"bg-red-50 text-red-700",
    PAYMENT_INITIATED:"bg-purple-50 text-purple-700",
  };
  return <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${colors[status]||"bg-gray-50 text-gray-600"}`}>{status}</span>;
}

export default function AdminDashboard() {
  const [filter, setFilter] = useState("all");
  const { data: orders=[], isLoading } = useQuery({ queryKey:["admin","orders"], queryFn: orderApi.getAllOrders });

  const filtered = filter==="all" ? orders : orders.filter((o: Order)=>o.status===filter);
  const revenue  = orders.filter((o: Order)=>o.status==="PAID"||o.status==="SHIPPED"||o.status==="DELIVERED")
    .reduce((s: number,o: Order)=>s+o.totalAmount, 0);

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">Admin Dashboard</h1>
      <div className="grid grid-cols-4 gap-4 mb-8">
        {[
          { label:"Total orders", value: orders.length },
          { label:"Revenue (paid+)", value: "$"+revenue.toFixed(0) },
          { label:"Pending",  value: orders.filter((o: Order)=>o.status==="PENDING").length },
          { label:"Cancelled", value: orders.filter((o: Order)=>o.status==="CANCELLED").length },
        ].map(m=>(
          <div key={m.label} className="bg-gray-50 rounded-xl p-4">
            <p className="text-xs text-gray-500 mb-1">{m.label}</p>
            <p className="text-2xl font-semibold text-gray-900">{m.value}</p>
          </div>
        ))}
      </div>
      <div className="bg-white rounded-xl border border-gray-100">
        <div className="flex items-center justify-between p-4 border-b border-gray-100">
          <h2 className="font-medium text-gray-900">Orders</h2>
          <select value={filter} onChange={e=>setFilter(e.target.value)}
            className="text-sm border border-gray-200 rounded-lg px-3 py-1.5 focus:outline-none">
            <option value="all">All statuses</option>
            {["PENDING","VALIDATED","PAYMENT_INITIATED","PAID","SHIPPED","DELIVERED","CANCELLED"].map(s=>(
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
        {isLoading ? <div className="p-8 text-center text-gray-400">Loading…</div> : (
          <table className="w-full text-sm">
            <thead><tr className="text-left text-xs text-gray-500 border-b border-gray-100">
              {["Order ID","Customer","Amount","Status","Created",""].map(h=>(
                <th key={h} className="px-4 py-3 font-medium">{h}</th>
              ))}
            </tr></thead>
            <tbody>
              {filtered.map((o: Order) => (
                <tr key={o.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{o.id.slice(0,8)}…</td>
                  <td className="px-4 py-3">{o.id.slice(0,8)}</td>
                  <td className="px-4 py-3 font-medium">${o.totalAmount.toFixed(2)}</td>
                  <td className="px-4 py-3"><StatusBadge status={o.status}/></td>
                  <td className="px-4 py-3 text-xs text-gray-400">{new Date(o.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3">
                    <button className="text-xs text-purple-600 hover:underline">View</button>
                  </td>
                </tr>
              ))}
              {!filtered.length && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">No orders found.</td></tr>}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
