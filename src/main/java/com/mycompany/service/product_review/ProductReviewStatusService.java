package com.mycompany.service.product_review;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mycompany.model.product_review.ProductReview;
import com.mycompany.model.product_review.ProductReviewStatus;
import com.mycompany.model.product_review.ProductReviewStatusDao;
import com.mycompany.model.product_review.Status;
import com.mycompany.model.product_review.StatusDao;

@Service
public class ProductReviewStatusService
{
	public static final String STATUS_SUBMITTED = "SUBMITTED";
	public static final String STATUS_PROCESSING = "PROCESSING";
	public static final String STATUS_PUBLISHED = "PUBLISHED";
	public static final String STATUS_ARCHIVED = "ARCHIVED";
	public static final String STATUS_ERROR = "ERROR";
	
	@Autowired
	private StatusDao statusDao;
	
	public ProductReviewStatus buildProductReviewStatus(String statusCode, String comments)
	{
		Status status = statusDao.getByCode(statusCode);
		ProductReviewStatus productReviewStatus = new ProductReviewStatus();
		productReviewStatus.setStatus(status);
		productReviewStatus.setComments(comments);
		productReviewStatus.setIsLast(true);
		return productReviewStatus;
	}
	
	public ProductReviewStatus buildProductReviewStatusSubmitted(String comments)
	{
		return buildProductReviewStatus(STATUS_SUBMITTED, comments);
	}
	
	public ProductReviewStatus buildProductReviewStatusProcessing(String comments)
	{
		return buildProductReviewStatus(STATUS_PROCESSING, comments);
	}

	public ProductReviewStatus buildProductReviewStatusPublished(String comments)
	{
		return buildProductReviewStatus(STATUS_PUBLISHED, comments);
	}

	public ProductReviewStatus buildProductReviewStatusArchived(String comments)
	{
		return buildProductReviewStatus(STATUS_ARCHIVED, comments);
	}

	public ProductReviewStatus buildProductReviewStatusError(String comments)
	{
		return buildProductReviewStatus(STATUS_ERROR, comments);
	}
	
	public void setCurrentProductReviewStatus(ProductReview productReview, ProductReviewStatus newProductReviewStatus)
	{
		ProductReviewStatus currentProductReviewStatus = productReview.getCurrentProductReviewStatus();
		if (currentProductReviewStatus != null)
		{
			currentProductReviewStatus.setIsLast(false);
			currentProductReviewStatus.setEndDate(new Date());
		}
		newProductReviewStatus.setProductReview(productReview);
		productReview.addProductReviewStatus(newProductReviewStatus);
	}
	
}
