package com.mycompany.service.product_review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycompany.api.jedis.RedisQueuePublisher;
import com.mycompany.model.product.Product;
import com.mycompany.model.product.ProductDao;
import com.mycompany.model.product_review.ProductReview;
import com.mycompany.model.product_review.ProductReviewDao;

@Service
@Transactional
public class ProductReviewService
{
	@Autowired
	private ProductReviewDao productReviewDao;
	
	@Autowired
	private ProductDao productDao;
	
	@Autowired
	private RedisQueuePublisher productReviewQueuePublisher;

	public ProductReview createProductReview(Integer productId, String reviewerName, String reviewerEmail, String reviewText, Integer rating)
	{
		Product product = productDao.getById(productId);
		
		ProductReview productReview = new ProductReview();
		productReview.setProduct(product);
		productReview.setReviewerName(reviewerName);
		productReview.setEmailAddress(reviewerEmail);
		productReview.setComments(reviewText);
		productReview.setRating(rating);
		
		productReviewDao.save(productReview);
		
		productReviewQueuePublisher.publish("product_review", productReview.toString());

		return productReview;
	}
}
