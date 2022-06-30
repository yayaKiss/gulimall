package com.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    private OSSClient ossClient;

    @Test
    public void testUpload() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("C:\\Users\\lenovo\\Pictures\\Saved Pictures\\3.png");
        ossClient.putObject("gulimall-lijie","feng.png",inputStream);
        System.out.println("上传成功...");
    }

}
