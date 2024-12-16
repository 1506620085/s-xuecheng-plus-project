package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuecheng.media.model.po.MqMessageHistory;
import com.xuecheng.media.service.MqMessageHistoryService;
import com.xuecheng.media.mapper.MqMessageHistoryMapper;
import org.springframework.stereotype.Service;

/**
* @author Hang
* @description 针对表【mq_message_history】的数据库操作Service实现
* @createDate 2024-12-16 22:59:13
*/
@Service
public class MqMessageHistoryServiceImpl extends ServiceImpl<MqMessageHistoryMapper, MqMessageHistory>
    implements MqMessageHistoryService{

}




