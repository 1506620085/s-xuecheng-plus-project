package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.media.model.po.MqMessage;
import com.xuecheng.media.service.MqMessageService;
import com.xuecheng.media.mapper.MqMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author Hang
* @description 针对表【mq_message】的数据库操作Service实现
* @createDate 2024-12-16 22:59:13
*/
@Service
public class MqMessageServiceImpl extends ServiceImpl<MqMessageMapper, MqMessage>
    implements MqMessageService{

}




