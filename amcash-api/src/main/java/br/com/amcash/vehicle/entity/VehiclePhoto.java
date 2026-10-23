package br.com.amcash.vehicle.entity;

import jakarta.persistence.*; import java.time.*;

@Entity @Table(name="vehicle_photos")
public class VehiclePhoto {
    @Id @Column(name="vehicle_id") private java.util.UUID vehicleId;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @MapsId @JoinColumn(name="vehicle_id") private Vehicle vehicle;
    @Column(name="content_type",nullable=false,length=32) private String contentType;
    @Column(nullable=false,columnDefinition="bytea") private byte[] content;
    @Column(name="updated_at",nullable=false) private OffsetDateTime updatedAt;
    protected VehiclePhoto(){} public VehiclePhoto(Vehicle vehicle){this.vehicle=vehicle;}
    public void update(String contentType,byte[] content){this.contentType=contentType;this.content=content;updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public String getContentType(){return contentType;} public byte[] getContent(){return content;}
}
