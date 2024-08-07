package com.example.backend.rating.service;


import com.example.backend.common.BaseException;
import com.example.backend.common.CustomerLevel;
import com.example.backend.customer.model.entity.Customer;
import com.example.backend.customer.repository.CustomerRepository;
import com.example.backend.log.entity.LoginLog;
import com.example.backend.log.repository.LoginLogRespository;
import com.example.backend.orders.model.entity.Orders;
import com.example.backend.orders.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerRatingService {
    private final CustomerRepository customerRepository;
    private final OrdersRepository ordersRepository;
    private final LoginLogRespository loginLogRespository;

    public String rating() throws BaseException {
        List<Customer> customerList = customerRepository.findAll();
        List<Orders> ordersList;
        List<LoginLog> loginLogs;
        Integer totalAmount=0, count = 0;
        for(Customer customer : customerList){
            ordersList = ordersRepository.findByCustomerIdx(customer.getIdx());
            for(Orders orders : ordersList){
                totalAmount += orders.getProductPrice();
            }

            loginLogs  = loginLogRespository.findByCustomerIdx(customer.getIdx());
            for(LoginLog log : loginLogs) {
                count++;
            }

            CustomerLevel level = calculateLevel(totalAmount * 8 + count * 2);

            customer.setLevel(level);
            customerRepository.save(customer);
        }
        return "등급 산정이 완료되었습니다.";
    }

    public CustomerLevel calculateLevel(Integer score){
        CustomerLevel level;
        if (score >= 100000000){
            level = CustomerLevel.DIAMOND;
        } else if (score >= 8000000) {
            level = CustomerLevel.PLATINUM;
        } else if (score >= 600000) {
            level = CustomerLevel.GOLD;
        }else if (score >= 70000) {
            level = CustomerLevel.SILVER;
        }else if (score >= 30000) {
            level = CustomerLevel.BRONZE;
        }else {
            level = CustomerLevel.NEWBIE;
        }
        return level;
    }

}
