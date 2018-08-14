package com.mycompany.service.product_review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycompany.api.product_review.UserReview;
import com.mycompany.model.product.Product;
import com.mycompany.model.product.ProductDao;
import com.mycompany.model.product_review.ProductReview;
import com.mycompany.model.product_review.ProductReviewDao;
import com.mycompany.sdk.redis.RedisQueue;

@Service
@Transactional
public class ProductReviewService
{
	
	private final Logger log = LoggerFactory.getLogger(ProductReviewService.class);
	
	@Autowired
	private ProductReviewDao productReviewDao;
	
	@Autowired
	private ProductDao productDao;
	
	@Autowired
	private ProductReviewStatusService productReviewStatusService;
	
	@Autowired
	private RedisQueue submittedQueue;

	public ProductReview createProductReview(Integer productId, String reviewerName, String reviewerEmail, String reviewText, Integer rating)
	{
		Product product = productDao.getById(productId);
		
		ProductReview productReview = new ProductReview();
		productReview.setProduct(product);
		productReview.setReviewerName(reviewerName);
		productReview.setEmailAddress(reviewerEmail);
		productReview.setComments(reviewText);
		productReview.setRating(rating);
		productReviewStatusService.setCurrentProductReviewStatus(productReview, productReviewStatusService.buildProductReviewStatusSubmitted("Review just submitted"));
		
		productReview = productReviewDao.save(productReview);

		submittedQueue.publish(productReview.getProductReviewID().toString());
		log.info("Product Review saved and pushed to the Submitted Queue");
		return productReview;
	}

	public ProductReview createProductReview(UserReview userReview)
	{
		return createProductReview(userReview.getProductId(), 
				userReview.getReviewerName(), userReview.getReviewerEmailAddress(), 
					userReview.getReviewText(), userReview.getRating());
	}
	
}
