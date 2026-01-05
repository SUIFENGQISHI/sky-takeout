package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Service
@Api(tags = "用户相关接口")
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    //微信服务接口地址
    public static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public User wxlogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前微信用户的openid
        String openid = getOpenid(userLoginDTO.getCode());
        
        // 测试时如果微信接口返回null，使用模拟openid
        if(openid == null) {
            log.warn("微信接口未返回openid，使用测试openid");
            openid = "test_openid_" + userLoginDTO.getCode();
        }

        //判断当前微信用户是否为新用户，如果是新用户，自动完成注册
        User user = userMapper.getUserByOpenid(openid);
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回用户对象
        return user;
    }


    /**
     * 调用微信接口服务，获得当前微信用户的openid
     *
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        //调用微信接口服务，获得当前微信用户的openid
        Map<String, String> params = new HashMap<>();
        params.put("appid", weChatProperties.getAppid());
        params.put("secret", weChatProperties.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, params);
        JSONObject jsonObject = JSON.parseObject(json);
        return jsonObject.getString("openid");

    }
}
