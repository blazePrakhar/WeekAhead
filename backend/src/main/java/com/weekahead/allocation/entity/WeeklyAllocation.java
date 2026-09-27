package com.weekahead.allocation.entity;

import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.week.entity.Week;
import jakarta.persistence.*;

@Entity
@Table(
        name = "weekly_allocations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weekly_allocations_week_life_area",
                        columnNames = {"week_id", "life_area_id"}
                )
        }
)
public class WeeklyAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "life_area_id", nullable = false)
    private LifeArea lifeArea;

    @Column(name = "recommended_minutes", nullable = false)
    private Integer recommendedMinutes;

    @Column(name = "planned_minutes", nullable = false)
    private Integer plannedMinutes;

    @Column(name = "actual_minutes", nullable = false)
    private Integer actualMinutes;

    @Column(name = "algorithm_version", nullable = false, length = 50)
    private String algorithmVersion;

    @Column(length = 1000)
    private String explanation;

    protected WeeklyAllocation() {
    }

    public WeeklyAllocation(
            Week week,
            LifeArea lifeArea,
            Integer recommendedMinutes,
            Integer plannedMinutes,
            Integer actualMinutes,
            String algorithmVersion,
            String explanation
    ) {
        this.week = week;
        this.lifeArea = lifeArea;
        this.recommendedMinutes = recommendedMinutes;
        this.plannedMinutes = plannedMinutes;
        this.actualMinutes = actualMinutes;
        this.algorithmVersion = algorithmVersion;
        this.explanation = explanation;
    }

    public Long getId() {
        return id;
    }

    public Week getWeek() {
        return week;
    }

    public LifeArea getLifeArea() {
        return lifeArea;
    }

    public Integer getRecommendedMinutes() {
        return recommendedMinutes;
    }

    public Integer getPlannedMinutes() {
        return plannedMinutes;
    }

    public Integer getActualMinutes() {
        return actualMinutes;
    }

    public String getAlgorithmVersion() {
        return algorithmVersion;
    }

    public String getExplanation() {
        return explanation;
    }

    public void updateRecommendation(
            Integer recommendedMinutes,
            String algorithmVersion,
            String explanation
    ) {
        this.recommendedMinutes = recommendedMinutes;
        this.algorithmVersion = algorithmVersion;
        this.explanation = explanation;
    }
}