package com.sky.service;

import com.sky.dto.DishDTO;
import io.swagger.annotations.Api;


public interface DishService {
    Long saveWithFlavors(DishDTO dishDTO);
}
