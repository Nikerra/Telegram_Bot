package io.project.telegram_bot.model.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "ads_table")
public class Ads {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "ad")
    private String ad;

}




