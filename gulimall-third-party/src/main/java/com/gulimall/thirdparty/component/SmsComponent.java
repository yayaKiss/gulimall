package com.gulimall.thirdparty.component;

import com.gulimall.thirdparty.utils.HttpUtils;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
@ConfigurationProperties(prefix = "spring.cloud.alicloud")
public class SmsComponent {
    private String host;
    private String path;
    private String appcode;
    private String templateId;

    public void sendMessage(String mobile,String code){
//        String host = "https://jumsendsms.market.alicloudapi.com";
//        String path = "/sms/send-upgrade";
        String method = "POST";
//        String appcode = "552ac12841954322acde17bf4b31b10d";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", mobile);
        querys.put("templateId", templateId);
        querys.put("value", code);
        Map<String, String> bodys = new HashMap<String, String>();


        try {

            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
