/**
 * Copyright 2022 bejson.com
 */
package com.gulimall.auth.vo;

import lombok.Data;

/**
 * Auto-generated: 2022-08-27 18:11:56
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
@Data
public class SocialUser {

    private String access_token;
    private String remind_in;
    private long expires_in;
    private String uid;
    private String isRealName;


}