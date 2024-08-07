package com.example.backend.email.service;

import com.example.backend.common.BaseException;
import com.example.backend.customer.model.entity.Customer;
import com.example.backend.customer.repository.CustomerRepository;
import com.example.backend.email.model.entity.Email;
import com.example.backend.email.repository.EmailRepository;
import com.example.backend.product.model.entity.Product;
import com.example.backend.product.model.response.GetProductRecRes;
import com.example.backend.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final ProductService productService;
    private final CustomerRepository customerRepository;
    private final EmailRepository emailRepository;

    public void sendEmails() {
        try {
            //:TODO 현재 - 모든 고객에게 이메일 전송 -> 보내는 대상 설정 필요
            GetProductRecRes getProductRecRes = productService.recommend();
            Map<String, Map<Product, Double>> productList = getProductRecRes.getRecommendList();
            for (Map.Entry<String, Map<Product, Double>> entry : productList.entrySet()) {
                sendMail(entry.getKey(), productList);
            }
        } catch (MessagingException | BaseException e) {
            e.printStackTrace();
        }
    }


    private void sendMail(String username, Map<String, Map<Product, Double>> productList) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        Optional<Customer> result = customerRepository.findByCustomerEmail(username);
        if (result.isPresent()) {
            Customer customer = result.get();


            helper.setSubject("[광고] CuStard :) 고객님을 위한 추천 상품!");
            helper.setTo(""); //:TODO 고객 username(이메일)로 변경
            Context context = new Context();

            int i = 1;

            Map<Product, Double> productMap = productList.get(username);
            for (Map.Entry<Product, Double> productEntry : productMap.entrySet()) {
                if (i > 4) break;
                Product product = productEntry.getKey();
                context.setVariable("name", username);
                context.setVariable("product" + i, product.getProductName());
                context.setVariable("product" + i + "Price", product.getProductPrice());
                context.setVariable("product" + i + "Image", product.getProductImage());
                i++;
            }

            String html = templateEngine.process("mail/mail", context);
            helper.setText(html, true);
            javaMailSender.send(message);

            Document doc = Jsoup.parse(html);
            Elements sections = doc.select("section");
            StringBuilder sectionHtml = new StringBuilder();

            for (Element section : sections) {
                sectionHtml.append(section.outerHtml()); // outerHtml()을 사용하여 요소 전체를 문자열로 가져옴
            }
            String sectionsAsString = sectionHtml.toString();


            //:TODO admin을 시스템이 배치 처리로 전송할 경우 0으로 설정
            emailRepository.save(Email.builder().emailContent(sectionsAsString).customer(customer).admin(null).build());
        } else {
                //사용자가 존재하지 않을 경우 오류 처리
        }
    }
    public void sendSleeperEmail(List<Long> targetList) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        for (Long targetidx : targetList) {
            Optional<Customer> result = customerRepository.findById(targetidx);
            if (result.isPresent()) {
                Customer customer = result.get();
                helper.setSubject("[광고] CuStard :) 고객님만을 위한 쿠폰이 발급되었어요!");
                helper.setTo(customer.getCustomerEmail());
                Context context = new Context();
                context.setVariable("username", customer.getUsername());

                String html = templateEngine.process("mail/sleeper", context);
                helper.setText(html, true);

                javaMailSender.send(message);

                //:TODO admin을 시스템이 배치 처리로 전송할 경우 0으로 설정
                emailRepository.save(Email.builder().emailContent(html).customer(customer).admin(null).build());
            } else {
                //사용자가 존재하지 않을 경우 오류 처리
            }
        }
    }

    public void sendLevelCoupon(List<List<Long>> targetList) throws MessagingException{
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        for (int i = 0; i < targetList.size(); i++) {
            for (Long targetidx:targetList.get(i)) {
                Optional<Customer> result = customerRepository.findById(targetidx);
                if (result.isPresent()) {
                    Customer customer = result.get();
                    helper.setSubject("[광고] CuStard :) 고객님만을 위한 쿠폰이 발급되었어요!");
                    helper.setTo(customer.getCustomerEmail());
                    Context context = new Context();
                    context.setVariable("level", customer.getLevel());

                    String html = templateEngine.process("mail/level", context);
                    helper.setText(html, true);

                    javaMailSender.send(message);

                    //:TODO admin을 시스템이 배치 처리로 전송할 경우 0으로 설정
                    emailRepository.save(Email.builder().emailContent(html).customer(customer).admin(null).build());
                } else {
                    //사용자가 존재하지 않을 경우 오류 처리
                }
            }
        }


    }
}