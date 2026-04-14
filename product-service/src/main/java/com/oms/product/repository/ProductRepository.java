package com.oms.product.repository;

import com.oms.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product,String> {
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByCategoryAndActiveTrue(String category, Pageable pageable);
    @Query("{'active':true,'$or':[{'name':{$regex:?0,$options:'i'}},{'description':{$regex:?0,$options:'i'}}]}")
    List<Product> searchByKeyword(String keyword);
    List<Product> findByCategoryAndActiveTrue(String category);
}
