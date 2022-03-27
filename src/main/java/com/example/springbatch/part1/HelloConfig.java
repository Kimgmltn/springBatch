package com.example.springbatch.part1;


import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class HelloConfig {

    // 스프링배치에서 Job을 만들수 있는 팩토리 제공
    private final JobBuilderFactory jobBuilderFactory;
    // 스프링배치에서 Step을 만들수 있는 팩토리 제공
    private final StepBuilderFactory stepBuilderFactory;

    public HelloConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    // 배치 실행 단위
    @Bean
    public Job helloJob(){
        /*
        * RunIdIncrementer은 Job 실행시, 파라미터 id를 자동으로 생성해줌
        * */
        return jobBuilderFactory.get("helloJob") // Job 이름. 스프링 배치를 실행할 수 있는 키
                .incrementer(new RunIdIncrementer()) // 실행 단위 구분
                .start(this.helloStep()) // Job 실행시 최초로 실행될 Step 설정
                .build();
    }

    @Bean
    public Step helloStep(){
        /*
        * Step은 Job의 실행 단위. 하나의 Job은 1개 이상의 Step을 가질 수 있음
        * */
        return stepBuilderFactory.get("helloStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("hello spring batch");
                    return RepeatStatus.FINISHED;
                }).build();
    }


}
