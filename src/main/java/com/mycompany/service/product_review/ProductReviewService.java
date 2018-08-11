package com.mycompany.service.product_review;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycompany.api.jedis.RedisMessagePublisher;
import com.mycompany.api.product_review.UserReview;
import com.mycompany.model.product.Product;
import com.mycompany.model.product.ProductDao;
import com.mycompany.model.product_review.ProductReview;
import com.mycompany.model.product_review.ProductReviewDao;
import com.mycompany.model.product_review.ProductReviewStatus;
import com.mycompany.model.product_review.Status;
import com.mycompany.model.product_review.StatusDao;

@Service
@Transactional
public class ProductReviewService
{
	public static final String STATUS_SUBMITTED = "SUBMITTED";
	public static final String STATUS_PROCESSING = "PROCESSING";
	public static final String STATUS_PUBLISHED = "PUBLISHED";
	public static final String STATUS_ARCHIVED = "ARCHIVED";
	public static final String STATUS_ERROR = "ERROR";
	
	public static final String QUEUE_SUBMITTED = "submitted_product_reviews_queue";
	public static final String QUEUE_PROCESSED = "processed_product_reviews_queue";
	public static final String QUEUE_NOT_PROCESSED = "processed_product_reviews_queue";

	@Autowired
	private ProductReviewDao productReviewDao;
	
	@Autowired
	private ProductDao productDao;
	
	@Autowired
	private StatusDao statusDao;
	
	@Autowired
	@Qualifier("productReviewQueuePublisher")
	private RedisMessagePublisher productReviewQueuePublisher;

	public ProductReview createProductReview(Integer productId, String reviewerName, String reviewerEmail, String reviewText, Integer rating)
	{
		Product product = productDao.getById(productId);
		
		ProductReview productReview = new ProductReview();
		productReview.setProduct(product);
		productReview.setReviewerName(reviewerName);
		productReview.setEmailAddress(reviewerEmail);
		productReview.setComments(reviewText);
		productReview.setRating(rating);
		productReview.setCurrentProductReviewStatus(buildProductReviewStatusSubmitted("Review just submitted"));
		
		productReviewDao.save(productReview);

		productReviewQueuePublisher.publish(productReview.getProductReviewID().toString());

		return productReview;
	}

	public ProductReview createProductReview(UserReview userReview)
	{
		return createProductReview(userReview.getProductId(), 
				userReview.getReviewerName(), userReview.getReviewerEmailAddress(), 
					userReview.getReviewText(), userReview.getRating());
	}
	
	public Status getStatusSubmitted()
	{
		return statusDao.getByCode(STATUS_SUBMITTED);
	}
	
	public Status getStatusProcessing()
	{
		return statusDao.getByCode(STATUS_PROCESSING);
	}

	public Status getStatusPublished()
	{
		return statusDao.getByCode(STATUS_PUBLISHED);
	}

	public Status getStatusArchived()
	{
		return statusDao.getByCode(STATUS_ARCHIVED);
	}
	
	public Status getStatusError()
	{
		return statusDao.getByCode(STATUS_ERROR);
	}
	
	public List<ProductReview> getProductAllReviews(Integer productID)
	{
		return productReviewDao.getList(productID);
	}
	
	public boolean isCommentContainsInappropriateLanguage(String comments)
	{
		//TODO Hardcoded, but can also be stored and loaded from Db or Redis
		Pattern p = Pattern.compile("(\\bfee\\b)|(\\bnee\\b)|(\\bcruul\\b)|(\\bleent\\b)");
		Matcher m = p.matcher(comments);
		return m.matches();
	}
	
	public boolean isProductReviewContainsInappropriateLanguage(ProductReview productReview)
	{
		if (productReview != null)
			return isCommentContainsInappropriateLanguage(productReview.getComments());
		else
			return false;
	}
	
	public boolean isProductReviewContainsInappropriateLanguage(Integer productReviewID)
	{
		return isProductReviewContainsInappropriateLanguage(productReviewDao.getByID(productReviewID));
	}
	
	public void processProductReview(Integer productReviewID)
	{
		ProductReview productReview = productReviewDao.getByID(productReviewID);
		if (productReview != null)
		{
			try
			{
				productReview.setCurrentProductReviewStatus(buildProductReviewStatusProcessing("Review is being checked for inuppropriate language"));
		
				boolean containsInappropriateLanguage = isProductReviewContainsInappropriateLanguage(productReview);
				
				if (containsInappropriateLanguage)
				{
					productReview.setCurrentProductReviewStatus(buildProductReviewStatusArchived("Review has not been passed inuppropriate language check"));
				}
				else
				{
					productReview.setCurrentProductReviewStatus(buildProductReviewStatusPublished("Review has been passed inuppropriate language check"));
				}
		
				productReviewQueuePublisher.publish(productReview.getProductReviewID().toString());
			}
			catch (Throwable e)
			{
				productReview.setCurrentProductReviewStatus(buildProductReviewStatusError("Review has not been processed due to system error." + e.getMessage()));
				productReviewQueuePublisher.publish(productReview.getProductReviewID().toString());
			}
		}
	}
	
	private ProductReviewStatus buildProductReviewStatus(String statusCode, String comments)
	{
		Status status = statusDao.getByCode(statusCode);
		ProductReviewStatus productReviewStatus = new ProductReviewStatus();
		productReviewStatus.setStatus(status);
		productReviewStatus.setComments(comments);
		return productReviewStatus;
	}
	
	private ProductReviewStatus buildProductReviewStatusSubmitted(String comments)
	{
		return buildProductReviewStatus(STATUS_SUBMITTED, comments);
	}
	
	private ProductReviewStatus buildProductReviewStatusProcessing(String comments)
	{
		return buildProductReviewStatus(STATUS_PROCESSING, comments);
	}

	private ProductReviewStatus buildProductReviewStatusPublished(String comments)
	{
		return buildProductReviewStatus(STATUS_PUBLISHED, comments);
	}

	private ProductReviewStatus buildProductReviewStatusArchived(String comments)
	{
		return buildProductReviewStatus(STATUS_ARCHIVED, comments);
	}

	private ProductReviewStatus buildProductReviewStatusError(String comments)
	{
		return buildProductReviewStatus(STATUS_ERROR, comments);
	}
}
