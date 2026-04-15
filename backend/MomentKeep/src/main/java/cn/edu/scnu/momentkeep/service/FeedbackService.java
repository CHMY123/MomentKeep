package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.dto.request.FeedbackDTO;
import cn.edu.scnu.momentkeep.common.PageResult;
import cn.edu.scnu.momentkeep.vo.FeedbackVO;

public interface FeedbackService {
    
    void submitFeedback(FeedbackDTO dto);
    
    PageResult<FeedbackVO> getMyFeedback(Integer page, Integer size);
}
