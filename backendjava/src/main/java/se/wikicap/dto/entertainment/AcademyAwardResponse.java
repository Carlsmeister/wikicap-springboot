package se.wikicap.dto.entertainment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response wrapper for Academy Awards API data.
 * Contains edition info and categories with their nominees grouped together.
 */
@Setter
@Getter
public class AcademyAwardResponse {

    private AcademyAwardDTO.Edition edition;
    private List<AcademyAwardDTO.Category> categories;
    private List<CategoryWithNominees> awards;

    public AcademyAwardResponse() {
    }

    /**
     * Inner class to group a category with its nominees.
     * Makes it clear which nominees belong to which category.
     */
    @Setter
    @Getter
    public static class CategoryWithNominees {
        private AcademyAwardDTO.Category category;
        private List<AcademyAwardDTO.Nominee> nominees;

        public CategoryWithNominees(AcademyAwardDTO.Category category, List<AcademyAwardDTO.Nominee> nominees) {
            this.category = category;
            this.nominees = nominees;
        }
    }
}
