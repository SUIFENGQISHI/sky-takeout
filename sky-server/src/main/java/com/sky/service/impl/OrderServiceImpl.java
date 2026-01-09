package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.Address;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;


    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //先判断地址是否存在，若不存在，抛出业务异常
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //再判断购物车是否为空，若是空，抛出业务异常
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(BaseContext.getCurrentId()).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //符合条件，先往订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setAddress(addressBook.toString());
        orders.setOrderTime(LocalDateTime.now());
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(BaseContext.getCurrentId());
        orders.setUserName(addressBook.getConsignee());
        orders.setConsignee(addressBook.getConsignee());
        orderMapper.insert(orders);
        //获取新插入的订单的id
        Long orderId = orders.getId();
        //再往订单明细表插入多条数据
        List<OrderDetail> orderDetailsList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orderId);
            orderDetailsList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailsList);

        //清除购物车数据
        shoppingCartMapper.deleteAll();

        //返回VO对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orderId)
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .packAmount(orders.getPackAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
/*        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }
*/
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderNumber);
        return vo;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("订单分页查询：{}", ordersPageQueryDTO);
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        Long total = page.getTotal();
        List<Orders> records = page.getResult();
        List<OrderVO> list = new ArrayList<>();
        for (Orders order : records) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(order.getId()));
            list.add(orderVO);  // 添加到列表中
        }

        return new PageResult(total, list);  // 返回封装后的分页结果
    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderById(Long id) {
        Orders orders = orderMapper.getByid(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(id));
        return orderVO;
    }

    /**
     * 订单取消
     *
     * @param id
     * @return
     */
    @Override
    public void rejectOrCancelOrder(Long id, String reason) {
        //判断当前订单的订单状态，仅待接单和待付款的订单允许取消
        Orders orders = orderMapper.getByid(id);
        if (orders.getStatus() != Orders.TO_BE_CONFIRMED && orders.getStatus() != Orders.PENDING_PAYMENT) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //若允许取消，修改订单状态为已取消，修改订单取消时间。
        if(reason!=null&&!reason.equals("")){
            if(orders.getStatus()==Orders.TO_BE_CONFIRMED){
                orders.setRejectionReason(reason);
            }else{
                orders.setCancelReason(reason);
            }
        }else{
            orders.setCancelReason("用户取消");
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);

    }

    /**
     * 再来一单
     *
     * @param id
     */
    @Override
    public void repetition(Long id) {
        // 1. 根据订单id查询原订单
        Orders oldOrder = orderMapper.getByid(id);

        // 2. 创建新订单，复制原订单数据
        Orders newOrder = new Orders();
        BeanUtils.copyProperties(oldOrder, newOrder);

        // 3. 修改新订单的关键字段
        newOrder.setId(null); // 清空id，让数据库自动生成
        newOrder.setNumber(String.valueOf(System.currentTimeMillis())); // 生成新订单号
        newOrder.setStatus(Orders.PENDING_PAYMENT); // 订单状态：待付款
        newOrder.setPayStatus(Orders.UN_PAID); // 支付状态：未支付
        newOrder.setOrderTime(LocalDateTime.now()); // 下单时间：当前时间
        newOrder.setCheckoutTime(null); // 清空结账时间
        newOrder.setEstimatedDeliveryTime(LocalDateTime.now().plusHours(1)); // 预计送达时间：1小时后
        newOrder.setCancelReason(null); // 清空取消原因
        newOrder.setCancelTime(null); // 清空取消时间
        newOrder.setRejectionReason(null); // 清空拒绝原因
        newOrder.setDeliveryTime(null); // 清空送达时间

        // 4. 插入新订单
        orderMapper.insert(newOrder);

        // 5. 查询原订单的订单明细
        List<OrderDetail> oldOrderDetails = orderDetailMapper.getByOrderId(id);

        // 6. 复制订单明细并关联到新订单
        List<OrderDetail> newOrderDetails = oldOrderDetails.stream().map(oldDetail -> {
            OrderDetail newDetail = new OrderDetail();
            BeanUtils.copyProperties(oldDetail, newDetail);
            newDetail.setId(null); // 清空id
            newDetail.setOrderId(newOrder.getId()); // 关联新订单id
            return newDetail;
        }).collect(Collectors.toList());

        // 7. 批量插入新订单明细
        orderDetailMapper.insertBatch(newOrderDetails);
    }

    /**
     * 订单搜索接口
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //分页查询并取出订单列表
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<Orders> ordersList = page.getResult();
        //封装VO列表
        List<OrderVO> orderVOList = ordersList.stream().map(order -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            Long orderId = order.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
            List<String> nameAndNumList = orderDetailList.stream().map(detail -> detail.getName() + "*" + detail.getNumber()).collect(Collectors.toList());
            String orderDetail = String.join(",", nameAndNumList);
            orderVO.setOrderDetail(orderDetail);
            return orderVO;
        }).collect(Collectors.toList());
        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        OrderStatisticsVO orderStatisticsVO=OrderStatisticsVO.builder()
                .toBeConfirmed(toBeConfirmed)
                .confirmed(confirmed)
                .deliveryInProgress(deliveryInProgress)
                .build();
        return orderStatisticsVO;
    }

}
