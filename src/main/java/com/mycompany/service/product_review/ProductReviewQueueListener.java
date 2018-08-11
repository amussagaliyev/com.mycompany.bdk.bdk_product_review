package com.mycompany.service.product_review;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class ProductReviewQueueListener implements MessageListener
{
	//@Autowired
	//ProductReviewService productReviewService;
	
	@Override
	public void onMessage(Message message, byte[] pattern)
	{
		System.out.println(new String(message.getBody()));
		//productReviewService.processProductReview(Integer.valueOf(new String(message.getBody())));
	}
}
