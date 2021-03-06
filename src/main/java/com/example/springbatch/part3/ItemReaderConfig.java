package com.example.springbatch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ItemReaderConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    public ItemReaderConfig(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
    }

    @Bean
    public Job itemReaderJob() throws Exception {
        return this.jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(this.csvFileStep())
                .next(this.jdbcStep())
                .next(this.jdbcBatchItemWriterStep())
                .build();
    }

    @Bean
    public Step csvFileStep() throws Exception {
        return stepBuilderFactory.get("csvFileStep")
                .<Person,Person>chunk(10)
                .reader(this.csvFileItemReader())
                .writer(itemWriter())
                .build();
    }


    // csv ????????? ?????? reader
    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        // ????????? ?????? ??? ?????? lineMapper??????
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();

        // Person ????????? mapping?????? ?????? ??????
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id","name","age","address"); // Person??? ???????????? ??????
        lineMapper.setLineTokenizer(tokenizer);

        // Person??? mapping
        lineMapper.setFieldSetMapper(fieldSet -> {
            int id = fieldSet.readInt("id");
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");
            return new Person(id,name,age,address);
        });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader") // itemReader??? ??????
                .encoding("UTF-8")
                .resource(new ClassPathResource("test.csv")) // /resource/test.csv ????????? ?????????.
                .linesToSkip(1) // ????????? ?????? ?????? ?????????, ????????? ?????? ???????????????.
                .lineMapper(lineMapper) // mapper??? lineMapper??? ????????????.
                .build();
        itemReader.afterPropertiesSet(); // itemReader??? ????????? ?????? ?????? ???????????? ?????????
        return itemReader;

    }

    @Bean
    public Step jdbcStep() throws Exception {
        return stepBuilderFactory.get("jdbcStep")
                .<Person,Person>chunk(10)
                .reader(this.jdbcCursorItemReader())
                .writer(this.itemWriter())
                .build();
    }

    private JdbcCursorItemReader<Person> jdbcCursorItemReader() throws Exception {
        JdbcCursorItemReader<Person> itemReader = new JdbcCursorItemReaderBuilder<Person>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)
                .sql("select id,name,age,address from hs.public.person")
                .rowMapper((rs, rowNum) -> {
                    return new Person(rs.getInt(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4));
                })
                .build();
         itemReader.afterPropertiesSet();
         return itemReader;
    }

    @Bean
    public Step customItemReaderStep() {
        return this.stepBuilderFactory.get("customItemReaderStep")
                .<Person,Person>chunk(10)
                .reader(new CustomItemReader<>(getItem()))
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<Person> itemWriter() {
        return items -> log.info(items.stream()
                .map(Person::getName)
                .collect(Collectors.joining(","))
        );
    }

    private List<Person> getItem() {
        List<Person> items = new ArrayList<>();

        for(int i = 0 ; i < 10 ; i++){
            items.add(new Person(i+1,"test name"+i, "test age", "test address"));
        }
        return items;
    }

    @Bean
    public Step jdbcBatchItemWriterStep() throws Exception {
        return stepBuilderFactory.get("jdbcBatchItemWriterStep")
                .<Person,Person>chunk(10)
                .reader(new CustomItemReader<Person>(getItem()))
                .writer(this.jdbcBatchItemWriter())
                .build();
    }

    private ItemWriter<Person> jdbcBatchItemWriter() {
        JdbcBatchItemWriter<Person> itemWriter = new JdbcBatchItemWriterBuilder<Person>()
                .dataSource(dataSource)
                // Person ???????????? ??????????????? ???????????? ????????? ??? ?????? ?????? ??????
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("insert into person(name,age,address) values (:name,:age,:address)")
                .build();
        itemWriter.afterPropertiesSet();
        return itemWriter;
    }
}
