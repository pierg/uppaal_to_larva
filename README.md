# uppaal_to_larva

This project converts an XML file from Uppaal into a java and a LARVA file if you follow this rules : 

- Uppaal doesn't allow the doubles so you have to multiply all your numbers by a factor then declare this factor in the XML file like : "int factor=X", at the beginning of the first <declaration> tag.

- You have to declare all your environment values between 2 tags : "/*start environment values*/" and "/*end environment values*/"

- You can declare the simulation values between 2 tags : "/*start simulation values*/" and "/*end simulation values*/" 

- You have to declare all your functions between "/*start functions*/" and "/*end functions" tags.