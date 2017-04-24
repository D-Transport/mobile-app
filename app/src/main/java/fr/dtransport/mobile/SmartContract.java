package fr.dtransport.mobile;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

/**
 * Smart contract interface
 *
 * Android version
 * 
 * @author Mathieu Porcel & Victor Le
 */
public class SmartContract {
	public static SmartContract s = null;

	private Web3j web3j;

	private BigInteger gasPrice = Transaction.DEFAULT_GAS;
	private BigInteger gasLimit = BigInteger.valueOf(4712388);

	private String address;
	private String contractAddress;

	public SmartContract(String url, String address, String contractAddress) {
		this.address = address;
		this.contractAddress = contractAddress;
		web3j = Web3jFactory.build(new HttpService(url));
		try {
			Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().sendAsync().get();
			String clientVersion = web3ClientVersion.getWeb3ClientVersion();
			System.out.println("Web3 version: " + clientVersion);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public String toAddress(Type value) {
		return "0x" + ((Address) value).getValue().toString(16);
	}

	public Web3j getWeb3j() {
		return web3j;
	}

	public long getBalance(String addr) throws InterruptedException, ExecutionException {
		return web3j.ethGetBalance(addr, DefaultBlockParameterName.LATEST).sendAsync().get().getBalance().longValue();
	}

	/**
	 * @return Transaction count
	 */
	private BigInteger getNonce() throws InterruptedException, ExecutionException {
		Request<?, EthGetTransactionCount> request = web3j.ethGetTransactionCount(address,
				DefaultBlockParameterName.LATEST);
		EthGetTransactionCount ethGetTransactionCount = request.sendAsync().get();
		return ethGetTransactionCount.getTransactionCount();
	}

	/**
	 * Call function
	 */
	private boolean call(Function function, long price) throws InterruptedException, ExecutionException {
		// Transaction
		BigInteger value = BigInteger.valueOf(price);
		Transaction transaction = Transaction.createFunctionCallTransaction(address, getNonce(), gasPrice, gasLimit,
				contractAddress, value, FunctionEncoder.encode(function));
		EthSendTransaction transactionResponse = web3j.ethSendTransaction(transaction).sendAsync().get();

		// Parse response
		if (transactionResponse.getError() == null) {
			EthGetTransactionReceipt transactionReceipt = web3j
					.ethGetTransactionReceipt(transactionResponse.getTransactionHash()).sendAsync().get();
			if (transactionReceipt.getError() == null) {
				return true;
			} else {
				System.err.println(transactionReceipt.getError().getMessage());
			}
		} else {
			System.err.println(transactionResponse.getError().getMessage());
		}
		return false;
	}

	/**
	 * Constant function
	 */
	private EthCall request(Function function) throws InterruptedException, ExecutionException {
		Transaction transaction = Transaction.createEthCallTransaction(contractAddress,
				FunctionEncoder.encode(function));
		EthCall request = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();
		return request;
	}

	/**
	 * @param addr
	 *            Company address
	 * @param name
	 *            Name
	 * @return Success
	 */
	@SuppressWarnings("rawtypes")
	public boolean addCompany(String addr, String name) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Address(addr));
		inputParam.add(new Utf8String(name));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();

		return call(new Function("addCompany", inputParam, outputParam), 0);
	}

	/**
	 * @param index
	 *            Company index
	 * @return Company (address, date, name, location)
	 */
	@SuppressWarnings("rawtypes")
	public List<Type> getCompany(long index) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Uint(BigInteger.valueOf(index)));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Address>() {
		});
		outputParam.add(new TypeReference<Uint>() {
		});
		outputParam.add(new TypeReference<Utf8String>() {
		});
		outputParam.add(new TypeReference<Utf8String>() {
		});

		// Request
		Function function = new Function("getCompany", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			return FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
		} else {
			System.err.println(request.getError().getMessage());
		}
		return null;
	}

	/**
	 * @return Company count
	 */
	@SuppressWarnings("rawtypes")
	public long getCompanyCount() throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Uint>() {
		});

		// Request
		Function function = new Function("getCompanyCount", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			List<Type> result = FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
			return ((BigInteger) result.get(0).getValue()).longValue();
		} else {
			System.err.println(request.getError().getMessage());
		}
		return 0;
	}

	/**
	 * @param addr
	 *            Terminal address
	 * @param location
	 *            Location
	 * @param company
	 *            Company address
	 * @return Success
	 */
	@SuppressWarnings("rawtypes")
	public boolean addTerminal(String addr, long location, String company)
			throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Address(addr));
		inputParam.add(new Uint(BigInteger.valueOf(location)));
		inputParam.add(new Address(company));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();

		return call(new Function("addTerminal", inputParam, outputParam), 0);
	}

	/**
	 * @param index
	 *            Company index
	 * @return Terminal (address, date, location, company)
	 */
	@SuppressWarnings("rawtypes")
	public List<Type> getTerminal(long index) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Uint(BigInteger.valueOf(index)));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Address>() {
		});
		outputParam.add(new TypeReference<Uint>() {
		});
		outputParam.add(new TypeReference<Uint>() {
		});
		outputParam.add(new TypeReference<Address>() {
		});

		// Request
		Function function = new Function("getTerminal", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			return FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
		} else {
			System.err.println(request.getError().getMessage());
		}
		return null;
	}

	/**
	 * @return Company count
	 */
	@SuppressWarnings("rawtypes")
	public long getTerminalCount() throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Uint>() {
		});

		// Request
		Function function = new Function("getTerminalCount", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			List<Type> result = FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
			return ((BigInteger) result.get(0).getValue()).longValue();
		} else {
			System.err.println(request.getError().getMessage());
		}
		return 0;
	}

	/**
	 * @param addr
	 *            Terminal address
	 * @param price
	 *            Price
	 * @return Success
	 */
	@SuppressWarnings("rawtypes")
	public boolean validate(String addr, long price) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Address(addr));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Bool>() {
		});

		return call(new Function("validate", inputParam, outputParam), price);
	}

	/**
	 * @return Success
	 */
	@SuppressWarnings("rawtypes")
	public boolean register() throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();

		return call(new Function("register", inputParam, outputParam), 0);
	}

	/**
	 * @param addr
	 *            User address
	 * @return Success
	 */
	@SuppressWarnings("rawtypes")
	public boolean giveAuthorization(String addr) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Address(addr));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();

		return call(new Function("giveAuthorization", inputParam, outputParam), 0);
	}

	/**
	 * @param index
	 *            User index
	 * @return User (address, date, validationCount)
	 */
	@SuppressWarnings("rawtypes")
	public List<Type> getUser(long index) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Uint(BigInteger.valueOf(index)));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Address>() {
		});
		outputParam.add(new TypeReference<Uint>() {
		});
		outputParam.add(new TypeReference<Uint>() {
		});

		// Request
		Function function = new Function("getUser", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			return FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
		} else {
			System.err.println(request.getError().getMessage());
		}
		return null;
	}

	/**
	 * @return User count
	 */
	@SuppressWarnings("rawtypes")
	public long getUsersCount() throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Uint>() {
		});

		// Request
		Function function = new Function("getUsersCount", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			List<Type> result = FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
			return ((BigInteger) result.get(0).getValue()).longValue();
		} else {
			System.err.println(request.getError().getMessage());
		}
		return 0;
	}

	/**
	 * @param addr
	 *            User address
	 * @param terminal
	 *            Terminal address
	 * @return Authorization Date
	 */
	@SuppressWarnings("rawtypes")
	public long getAuthorizationDate(String addr, String terminal) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Address(addr));
		inputParam.add(new Address(terminal));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Uint>() {
		});

		// Request
		Function function = new Function("getAuthorizationDate", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			List<Type> result = FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
			return ((BigInteger) result.get(0).getValue()).longValue();
		} else {
			System.err.println(request.getError().getMessage());
		}
		return 0;
	}

	/**
	 * @param addr
	 *            User address
	 * @param index
	 *            Validation index
	 * @return Validation (date, terminal)
	 */
	@SuppressWarnings("rawtypes")
	public List<Type> getValidation(String addr, long index) throws InterruptedException, ExecutionException {
		// Input parameters
		List<Type> inputParam = new ArrayList<>();
		inputParam.add(new Address(addr));
		inputParam.add(new Uint(BigInteger.valueOf(index)));

		// Output parameters
		List<TypeReference<?>> outputParam = new ArrayList<>();
		outputParam.add(new TypeReference<Uint>() {
		});
		outputParam.add(new TypeReference<Address>() {
		});

		// Request
		Function function = new Function("getValidation", inputParam, outputParam);
		EthCall request = request(function);
		if (request.getError() == null) {
			return FunctionReturnDecoder.decode(request.getResult(), function.getOutputParameters());
		} else {
			System.err.println(request.getError().getMessage());
		}
		return null;
	}
}
