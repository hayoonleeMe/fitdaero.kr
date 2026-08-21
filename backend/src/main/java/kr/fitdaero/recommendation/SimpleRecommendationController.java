package kr.fitdaero.recommendation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class SimpleRecommendationController {

  private final SimpleRecommendationService simpleRecommendationService;

  @PostMapping("/simple")
  public SimpleRecommendationApiResponse recommend(
      @Valid @RequestBody SimpleRecommendationApiRequest request) {
    return SimpleRecommendationApiResponse.from(
        simpleRecommendationService.recommend(request.toServiceRequest()));
  }
}
