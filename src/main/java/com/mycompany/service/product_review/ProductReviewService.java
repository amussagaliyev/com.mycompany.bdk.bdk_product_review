package com.mycompany.service.product_review;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mycompany.api.jedis.RedisQueuePublisher;
import com.mycompany.api.product_review.UserReview;
import com.mycompany.model.product.Product;
import com.mycompany.model.product.ProductDao;
import com.mycompany.model.product_review.ProductReview;
import com.mycompany.model.product_review.ProductReviewDao;
import com.mycompany.model.product_review.ProductReviewStatus;
import com.mycompany.model.product_review.ProductReviewStatusDao;
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
	private ProductReviewStatusDao productReviewStatusDao;

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
		setProductReviewCurrentStatusSubmitted(productReview);

		productReviewQueuePublisher.addToQueue(QUEUE_SUBMITTED, productReview.getProductReviewID().toString());

		return productReview;
	}

	public ProductReview createProductReview(UserReview userReview)
	{
		return createProductReview(userReview.getProductId(), 
				userReview.getReviewerName(), userReview.getReviewerEmailAddress(), 
					userReview.getReviewText(), userReview.getRating());
	}
	
	public void setProductReviewCurrentStatus(ProductReview productReview, Status status, String comments)
	{
		ProductReviewStatus currentStatus = new ProductReviewStatus();
		currentStatus.setStatus(status);
		currentStatus.setProductReview(productReview);
		currentStatus.setComments(comments);
		productReviewStatusDao.save(currentStatus);
	}
	
	public void setProductReviewCurrentStatusSubmitted(ProductReview productReview)
	{
		setProductReviewCurrentStatus(productReview, getStatusSubmitted(), "Just submitted");
	}

	public void setProductReviewCurrentStatusProcessing(ProductReview productReview)
	{
		setProductReviewCurrentStatus(productReview, getStatusProcessing(), "Being checked for inappropriate language");
	}

	public void setProductReviewCurrentStatusPublished(ProductReview productReview)
	{
		setProductReviewCurrentStatus(productReview, getStatusPublished(), "Passed inappropriate language check");
	}

	public void setProductReviewCurrentStatusArchived(ProductReview productReview)
	{
		setProductReviewCurrentStatus(productReview, getStatusArchived(), "Has not passed inappropriate language check and has been archived");
	}
	
	public void setProductReviewCurrentStatusError(ProductReview productReview, Throwable e)
	{
		StringWriter out = new StringWriter();
		PrintWriter printWriter = new PrintWriter(out);
		e.printStackTrace(printWriter);
		setProductReviewCurrentStatus(productReview, getStatusError(), "Has not been processed due to system error. " + out.toString());
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
				setProductReviewCurrentStatusProcessing(productReview);
		
				boolean containsInappropriateLanguage = isProductReviewContainsInappropriateLanguage(productReview);
				
				if (containsInappropriateLanguage)
				{
					setProductReviewCurrentStatusArchived(productReview);
				}
				else
				{
					setProductReviewCurrentStatusPublished(productReview);
				}
		
				productReviewQueuePublisher.addToQueue(QUEUE_PROCESSED, productReview.getProductReviewID().toString());
			}
			catch (Throwable e)
			{
				setProductReviewCurrentStatusError(productReview, e);
				productReviewQueuePublisher.addToQueue(QUEUE_NOT_PROCESSED, productReview.getProductReviewID().toString());
			}
		}
	}
}
