<!DOCTYPE html>
<html>
<head>
    <title>Multimodule Springboot Initializer</title>
    <style>
        body {
            font-family: Verdana, Geneva, sans-serif;
            background-color: #e7f7ff;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
        }

        form {
            width: 400px;
            margin: 0 auto;
            padding: 40px;
            background-color: white;
            border-radius: 8px;
            box-shadow: 0px 1px 6px rgba(0, 0, 0, 0.12);
        }

        label {
            display: block;
            font-size: 14px;
            margin-bottom: 5px;
        }

        input[type="text"],
        select {
            font-family: Verdana, Geneva, sans-serif;
            width: 100%;
            padding: 15px;
            font-size: 14px;
            border: 2px solid #F5F5F5;
            border-radius: 15px;
            background-color:#F5F5F5;
            color: black;
            transition-duration: 1s;
        }

        input[type="text"]:hover,
        select:hover {
            background-color:#DEDEDE;
        }

        .select-wrapper {
            display: inline-block;
            position: relative;
            width: 100%;
            margin-bottom: 10px;
        }

        .select-wrapper select {
            font-family: Verdana, Geneva, sans-serif;
            width: 100%;
            cursor: pointer;
            appearance: none;
            -webkit-appearance: none;
            -moz-appearance: none;
            padding-right: 30px;
        }

        .select-icon {
            position: absolute;
            top: 60%;
            right: 10px;
            transform: translateY(-50%);
            pointer-events: none;
        }

        input[type="submit"] {
            font-family: Verdana, Geneva, sans-serif;
            background-color: dodgerblue;
            color: white;
            padding: 15px 175px;
            border: none;
            border-radius: 15px;
            cursor: pointer;
            font-size: 14px;
            margin-top: 10px;
            transition-duration: 1s;
        }

        input[type="submit"]:hover {
            background-color: #0056b3;
        }

        h1 {
            font-size: 23px;
            font-weight: 500;
        }
    </style>
</head>
<body>
    <form method="post" action="/multispring/processForm">
        <h1>Multimodule Springboot Initializer</h1>
        <label>Application Name:</label>
        <input type="text" id="userInput" name="userInput" style="width: 91.5%;" required/>

        <!-- Dropdown button -->
        <div class="select-wrapper">
            <label>Java Version:</label>
            <select id="dropdownButton" name="dropdownButton">
                <option value="11">Java 11</option>
                <option value="17">Java 17</option>
            </select>
            <span class="select-icon">&#9662;</span>
        </div>

        <!-- Multiselect button -->
        <div class="select-wrapper">
            <label>Select Addons:</label>
            <select id="multiselectButton" name="multiselectButton" multiple>
                <option value="kafka">Kafka</option>
                <option value="ytd">Yet to Decide</option>
            </select>
        </div>

        <!-- Radio button -->
        <div class="select-wrapper">
             <label>Select Database:</label>
             <label>
                <input type="radio" name="addonOption" value="postgres" required> PostgreSQL
             </label>
             <label>
                 <input type="radio" name="addonOption" value="mongo"> Mongo DB
             </label>
        </div>

        <input type="submit" value="Submit" />
    </form>
</body>
</html>
 