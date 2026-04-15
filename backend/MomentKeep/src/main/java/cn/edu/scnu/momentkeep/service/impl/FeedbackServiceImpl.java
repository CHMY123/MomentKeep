package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.dto.request.FeedbackDTO;
import cn.edu.scnu.momentkeep.entity.Feedback;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.mapper.FeedbackMapper;
import cn.edu.scnu.momentkeep.service.FeedbackService;
import cn.edu.scnu.momentkeep.service.UserService;
import cn.edu.scnu.momentkeep.common.PageResult;
import cn.edu.scnu.momentkeep.vo.FeedbackVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    
    private final FeedbackMapper feedbackMapper;
    private final UserService userService;
    
    @Override
    public void submitFeedback(FeedbackDTO dto) {
        User currentUser = userService.getCurrentUser();
        
        Feedback feedback = new Feedback();
        feedback.setUserId(currentUser.getId());
        feedback.setContent(dto.getContent());
        // 如果未提供联系方式，默认使用用户的手机号
        feedback.setContact(dto.getContact() != null && !dto.getContact().isEmpty() 
            ? dto.getContact() 
            : currentUser.getPhone());
        feedback.setStatus(0); // 0表示未处理
        
        feedbackMapper.insert(feedback);
    }
    
    @Override
    public PageResult<FeedbackVO> getMyFeedback(Integer page, Integer size) {
        User currentUser = userService.getCurrentUser();
        
        // 构建查询条件
        QueryWrapper<Feedback> queryWrapper = new QueryWrapper<Feedback>()
                .eq("user_id", currentUser.getId())
                .orderByDesc("create_time");
        
        // 分页查询
        IPage<Feedback> feedbackPage = feedbackMapper.selectPage(
                new Page<>(page, size),
                queryWrapper
        );
        
        // 转换为VO
        List<FeedbackVO> feedbackVOs = feedbackPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return PageResult.of(feedbackVOs, feedbackPage.getTotal(), page, size);
    }
    
    private FeedbackVO convertToVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(feedback.getId());
        vo.setContent(feedback.getContent());
        vo.setContact(feedback.getContact());
        vo.setStatus(feedback.getStatus());
        vo.setReply(feedback.getReply());
        vo.setCreateTime(feedback.getCreateTime());
        return vo;
    }
}
