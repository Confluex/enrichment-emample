%dw 1.0
%output application/java
---
{
	addresses: payload.addresses map (( record) -> using (lookup=lookup("usps-city-state-lookup",{zip:record.zip}).CityStateLookupResponse.ZipCode) {
		name:record.name,
		city:lookup.City,
		state:lookup.State,
		zip:lookup.Zip5
	})
}
