package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Api("用户服务接口")
public interface UserService {


    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxlogin(UserLoginDTO userLoginDTO);
}
