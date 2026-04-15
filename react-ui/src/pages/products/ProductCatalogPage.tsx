import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { productApi, Product } from "../../api/productApi";
import { useAuth } from "../../auth/AuthContext";

function ProductCard({ product }: { product: Product }) {
  const { user } = useAuth();
  return (
    <div className="bg-white rounded-xl border border-gray-100 p-4 hover:shadow-md transition-shadow">
      <div className="h-40 bg-gray-50 rounded-lg mb-3 flex items-center justify-center text-gray-300 text-3xl">
        {product.imageUrl ? <img src={product.imageUrl} alt={product.name} className="h-full object-contain"/> : "📦"}
      </div>
      <span className="text-xs text-purple-600 bg-purple-50 px-2 py-0.5 rounded-full">{product.category}</span>
      <h3 className="font-medium text-gray-900 mt-2 text-sm leading-tight">{product.name}</h3>
      <p className="text-xs text-gray-500 mt-1 line-clamp-2">{product.description}</p>
      <div className="flex items-center justify-between mt-3">
        <span className="font-semibold text-gray-900">${product.price.toFixed(2)}</span>
        <span className={`text-xs ${product.stockQty > 0 ? "text-green-600" : "text-red-500"}`}>
          {product.stockQty > 0 ? `${product.stockQty} in stock` : "Out of stock"}
        </span>
      </div>
      {user ? (
        <Link to="/chat" state={{ productId: product.id, productName: product.name }}
          className="mt-3 block w-full py-2 rounded-lg bg-purple-700 text-white text-xs text-center hover:bg-purple-800">
          Order via AI chat
        </Link>
      ) : (
        <Link to="/login" className="mt-3 block w-full py-2 rounded-lg border border-purple-200 text-purple-700 text-xs text-center hover:bg-purple-50">
          Sign in to order
        </Link>
      )}
    </div>
  );
}

export default function ProductCatalogPage() {
  const [search, setSearch] = useState("");
  const [query,  setQuery]  = useState("");

  const { data, isLoading } = useQuery({
    queryKey: query ? ["products","search",query] : ["products","list"],
    queryFn:  query ? () => productApi.search(query).then(r=>({ content: r.results, totalElements: r.total, totalPages: 1 }))
                    : () => productApi.getAll(0,20),
  });

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">Product Catalog</h1>
        <p className="text-gray-500 text-sm">Browse freely — no account needed</p>
        <div className="flex gap-3 mt-4 max-w-lg">
          <input value={search} onChange={e=>setSearch(e.target.value)}
            onKeyDown={e=>e.key==="Enter"&&setQuery(search)}
            placeholder="Search products… (AI-powered)"
            className="flex-1 px-4 py-2.5 rounded-lg border border-gray-200 bg-white text-sm focus:outline-none focus:border-purple-400"/>
          <button onClick={()=>setQuery(search)}
            className="px-4 py-2.5 rounded-lg bg-purple-700 text-white text-sm hover:bg-purple-800">Search</button>
          {query && <button onClick={()=>{setQuery("");setSearch("");}} className="text-sm text-gray-400 hover:text-gray-600">Clear</button>}
        </div>
      </div>
      {isLoading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(8)].map((_,i)=><div key={i} className="h-64 bg-gray-100 rounded-xl animate-pulse"/>)}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {data?.content?.map(p=><ProductCard key={p.id} product={p}/>)}
          {!data?.content?.length && <p className="col-span-4 text-center text-gray-400 py-12">No products found.</p>}
        </div>
      )}
    </div>
  );
}
